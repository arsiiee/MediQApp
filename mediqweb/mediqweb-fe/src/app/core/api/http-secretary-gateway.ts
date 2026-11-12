import { Injectable, inject } from '@angular/core';
import { from, map, Observable, of, switchMap } from 'rxjs';
import { UserProfile, Appointment, MessageThread, Message, Doctor } from './models';
import { SupabaseService } from './supabase.service';
import { environment } from '../../../environments/environment';
import type { Conversation } from '../../secretary/secretary.models';
import type { ConversationMessage } from '../../secretary/secretary.models';

function toConversationMessage(m: Message): ConversationMessage {
  return {
    id: m.id,
    conversationId: m.conversationId,
    sentAt: m.sentAt,
    body: m.body,
    appointmentId: m.appointmentId ?? null,
    fromSecretary: m.authorRole === 'secretary',
    readAt: m.readAt ?? null,
  };
}

function toProfile(row: any): UserProfile {
  return {
    id: row.user_id,
    username: row.username,
    fullName: row.full_name ?? '',
    email: row.email ?? '',
    mobileNumber: row.email ?? '',
    dateOfBirth: '',
    role: (row.role ?? 'patient') as UserProfile['role'],
    status: row.status ?? 'ACTIVE',
    assignedDoctorId: row.assigned_doctor_id,
  };
}

function toThread(row: any): MessageThread {
  return {
    id: row.id,
    patientId: row.patient_id,
    doctorId: row.doctor_id,
    subject: row.subject,
    lastMessageAt: row.last_message_at,
    unreadCount: row.unread_count,
    awaitingAction: row.awaiting_action,
  };
}

function toMessage(row: any): Message {
  return {
    id: row.id,
    conversationId: row.conversation_id,
    authorId: row.author_id,
    authorRole: row.author_role,
    body: row.body,
    sentAt: row.sent_at,
    readAt: row.read_at ?? undefined,
  };
}

function toDoctor(row: any): Doctor {
  return {
    id: row.id,
    userId: row.user_id,
    fullName: row.full_name,
    specialty: row.specialty,
    specializationId: row.specialization_id,
    licenseNumber: row.license_number,
    yearsOfExperience: row.years_of_experience,
    feeCentavos: row.fee_centavos,
    bio: row.bio,
    building: row.building,
    floor: row.floor,
    room: row.room,
    languages: row.languages,
    status: row.status,
  };
}

@Injectable({ providedIn: 'root' })
export class HttpSecretaryGateway {
  private supabase = inject(SupabaseService);

  getAppointments(): Observable<Appointment[]> {
    return (from(
      (this.supabase as any)
        .from('appointments')
        .select('*')
        .then((r: any): any[] => r.data ?? []),
    ) as Observable<any[]>).pipe(
      map((data: any[]) => {
        return data.map((a: any) => ({
          id: a.id,
          patientId: a.patient_id,
          doctorId: a.doctor_id,
          startsAt: a.starts_at,
          durationMinutes: a.duration_minutes,
          status: String(a.status ?? '').toLowerCase(),
          reason: a.reason ?? '',
          createdAt: a.created_at,
          updatedAt: a.updated_at,
        }));
      }),
    );
  }

  confirmAppointment(id: string): Observable<void> {
    return from((this.supabase as any).rpc('confirm_appointment', { p_appointment_id: id })).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to confirm');
        }
      }),
    );
  }

  cancelAppointment(id: string, reason: string | null = null): Observable<void> {
    return from(
      (this.supabase as any).rpc('cancel_appointment', {
        p_appointment_id: id,
        p_cancel_reason: reason,
      }),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to cancel');
        }
      }),
    );
  }

  sendNotification(
    userId: string,
    type: string,
    title: string,
    body: string,
    data: string | null = null,
  ): Observable<void> {
    return from(
      (this.supabase as any).rpc('send_notification', {
        p_user_id: userId,
        p_type: type,
        p_title: title,
        p_body: body,
        p_data: data,
      }),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to send notification');
        }
      }),
    );
  }

  /** Send an FCM push notification via the Edge Function. Returns void on success. */
  sendPushNotification(
    fcmToken: string,
    title: string,
    body: string,
    clickAction: string | null = null,
    appointmentId: string | null = null,
  ): Observable<void> {
    const url = `${environment.supabaseUrl}/functions/v1/send-push`;
    return from(this.supabase.getAccessToken()).pipe(
      switchMap((accessToken) =>
        from(
          fetch(url, {
            method: 'POST',
            headers: {
              ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              fcm_token: fcmToken,
              title,
              body,
              click_action: clickAction,
              appointment_id: appointmentId,
            }),
          }),
        ),
      ),
      switchMap((resp: Response) => {
        if (!resp.ok) {
          return from(resp.text()).pipe(
            map((text) => {
              throw new Error(text || `send-push failed (${resp.status})`);
            }),
          );
        }
        return from(Promise.resolve());
      }),
    );
  }

  /** Look up a patient's FCM token from their profile. */
  getPatientFcmToken(userId: string): Observable<string | null> {
    return from(
      (this.supabase as any)
        .from('profiles')
        .select('fcm_token')
        .eq('user_id', userId)
        .maybeSingle(),
    ).pipe(
      map((r: any) => (r.error ? null : (r.data?.fcm_token ?? null))),
    );
  }

  getPatients(): Observable<UserProfile[]> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from((this.supabase as any).from('profiles').select('*').eq('role', 'PATIENT')).pipe(
          map((r: any) => {
            if (r.error) {
              throw new Error(r.error?.message ?? 'Failed to load patients');
            }
            return (r.data ?? []).map(toProfile);
          }),
        );
      }),
    );
  }

  getMessages(): Observable<MessageThread[]> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any)
            .from('users')
            .select('assigned_doctor_id')
            .eq('auth_user_id', result.data.user.id)
            .single(),
        ).pipe(
          switchMap((userResult: any) => {
            if (userResult.error || !userResult.data) {
              throw new Error('User not found');
            }
            const doctorId = userResult.data.assigned_doctor_id;
            if (!doctorId) {
              return of([]);
            }
            return (from(
                (this.supabase as any)
                    .from('message_threads')
                    .select('*')
                    .eq('doctor_id', doctorId)
                    .then((r: any): any[] => r.data ?? []),
            ) as Observable<any[]>).pipe(
                map((data: any[]) => {
                    return data.map(toThread);
                }),
            );
          }),
        );
      }),
    );
  }

  getConversation(id: string): Observable<Conversation> {
    return from(
      (this.supabase as any).from('message_threads').select('*').eq('id', id).single(),
    ).pipe(
      map((r: any) => {
        if (r.error || !r.data) {
          throw new Error(r.error?.message ?? 'Conversation not found');
        }
        const t = r.data;
        return {
          id: t.id,
          party: 'patient' as const,
          partyId: t.patient_id,
          awaitingAction: t.awaiting_action,
        } as Conversation;
      }),
    );
  }

  getConversationMessages(conversationId: string): Observable<ConversationMessage[]> {
    return from(
      (this.supabase as any)
        .from('messages')
        .select('*')
        .eq('conversation_id', conversationId)
        .order('sent_at', { ascending: true }),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to load messages');
        }
        return (r.data ?? []).map(toMessage).map(toConversationMessage);
      }),
    );
  }

  getDoctor(): Observable<Doctor | null> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        const authUserId = result.data.user.id;
        // Try 1: users table via auth_user_id bridge (Ktor domain model)
        return from(
          (this.supabase as any)
            .from('users')
            .select('assigned_doctor_id, username')
            .eq('auth_user_id', authUserId)
            .single(),
        ).pipe(
          switchMap((userResult: any) => {
            if (userResult.error || !userResult.data || !userResult.data.assigned_doctor_id) {
              // Try 2: profiles table assigned_doctor_id
              return from(
                (this.supabase as any)
                  .from('profiles')
                  .select('assigned_doctor_id')
                  .eq('user_id', authUserId)
                  .single(),
              ).pipe(
                switchMap((profileResult: any) => {
                  if (
                    profileResult.error ||
                    !profileResult.data ||
                    !profileResult.data.assigned_doctor_id
                  ) {
                    // Try 3: profiles username → users assigned_doctor_id
                    return from(
                      (this.supabase as any)
                        .from('profiles')
                        .select('username')
                        .eq('user_id', authUserId)
                        .single(),
                    ).pipe(
                      switchMap((profResult: any) => {
                        if (profResult.error || !profResult.data || !profResult.data.username) {
                          // Try 4: doctors table directly by user_id
                          return from(
                            (this.supabase as any)
                              .from('doctors')
                              .select('*')
                              .eq('user_id', authUserId)
                              .maybeSingle(),
                          ).pipe(
                            map((r: any) => {
                              if (r.error) {
                                throw new Error(r.error?.message ?? 'Doctor not found');
                              }
                              if (!r.data) {
                                return null;
                              }
                              return toDoctor(r.data);
                            }),
                          );
                        }
                        return from(
                          (this.supabase as any)
                            .from('users')
                            .select('assigned_doctor_id')
                            .eq('username', profResult.data.username)
                            .single(),
                        ).pipe(
                          switchMap((uResult: any) => {
                            if (
                              uResult.error ||
                              !uResult.data ||
                              !uResult.data.assigned_doctor_id
                            ) {
                              return of(null as Doctor | null);
                            }
                            const doctorId = uResult.data.assigned_doctor_id;
                            return from(
                              (this.supabase as any)
                                .from('doctors')
                                .select('*')
                                .eq('user_id', doctorId)
                                .maybeSingle(),
                            ).pipe(
                              map((r: any) => {
                                if (r.error) {
                                  throw new Error(r.error?.message ?? 'Doctor not found');
                                }
                                if (!r.data) {
                                  return null;
                                }
                                return toDoctor(r.data);
                              }),
                            );
                          }),
                        );
                      }),
                    );
                  }
                  const doctorId = profileResult.data.assigned_doctor_id;
                  return from(
                    (this.supabase as any)
                      .from('doctors')
                      .select('*')
                      .eq('user_id', doctorId)
                      .maybeSingle(),
                  ).pipe(
                    map((r: any) => {
                      if (r.error) {
                        throw new Error(r.error?.message ?? 'Doctor not found');
                      }
                      if (!r.data) {
                        return null;
                      }
                      return toDoctor(r.data);
                    }),
                  );
                }),
              );
            }
            const assignedDoctorId = userResult.data.assigned_doctor_id;
            return from(
              (this.supabase as any)
                .from('doctors')
                .select('*')
                .eq('user_id', assignedDoctorId)
                .maybeSingle(),
            ).pipe(
              map((r: any) => {
                if (r.error) {
                  throw new Error(r.error?.message ?? 'Doctor not found');
                }
                if (!r.data) {
                  return null;
                }
                return toDoctor(r.data);
              }),
            );
          }),
        );
      }),
    );
  }

  getDoctorSchedule(doctorId: string): Observable<any[]> {
    return from(
      (this.supabase as any).from('clinic_hours').select('*').eq('doctor_id', doctorId),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to load schedule');
        }
        return (r.data ?? [])
          .filter((d: any) => !d.is_closed && d.start_time && d.end_time)
          .map((d: any) => ({
            dayOfWeek: Number(d.day_of_week),
            startTime: String(d.start_time).slice(0, 5),
            endTime: String(d.end_time).slice(0, 5),
          }))
          .sort(
            (a: any, b: any) =>
              a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime),
          );
      }),
    );
  }

  sendMessage(
    conversationId: string,
    body: string,
    appointmentId: string | null = null,
  ): Observable<ConversationMessage> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any).rpc('send_message', {
            p_conversation_id: conversationId,
            p_body: body,
            p_appointment_id: appointmentId,
          }),
        ).pipe(
          switchMap((r: any) => {
            if (r.error) {
              throw new Error(r.error?.message ?? 'Failed to send message');
            }
            return this.getConversationMessages(conversationId).pipe(
              map((msgs) => msgs[msgs.length - 1]),
            );
          }),
        );
      }),
    );
  }

  markConversationRead(conversationId: string): Observable<void> {
    return from(
      (this.supabase as any)
        .from('message_threads')
        .update({ unread_count: 0, awaiting_action: false })
        .eq('id', conversationId),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to mark as read');
        }
      }),
    );
  }
}
