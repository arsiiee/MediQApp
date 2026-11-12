import { Injectable, inject } from '@angular/core';
import { catchError, from, map, Observable, of, switchMap } from 'rxjs';
import {
  Appointment,
  UserProfile,
  Doctor,
  Specialization,
  Paged,
  MedicalRecord,
  Prescription,
  Invoice,
  Notification,
  PaymentRequest,
  CreateAppointmentRequest,
  AppointmentFilter,
  ApiResponse,
} from './models';
import { SupabaseService } from './supabase.service';
import { environment } from '../../../environments/environment';
import type { ScheduleDay } from '../../doctor/doctor.models';
import type { DatedAvailability } from '../../doctor/doctor.models';
import type { VisitOutcome } from '../../doctor/doctor.models';

function toProfile(row: any): UserProfile {
  return {
    id: row.user_id ?? row.id,
    username: row.username ?? '',
    fullName: row.full_name ?? row.fullName ?? row.username ?? '',
    email: row.email ?? '',
    mobileNumber: row.mobile_number ?? row.mobileNumber ?? '',
    dateOfBirth: row.date_of_birth ?? row.dateOfBirth ?? '',
    role: (row.role ?? 'patient') as UserProfile['role'],
    status: row.status ?? 'ACTIVE',
    assignedDoctorId: row.assigned_doctor_id ?? row.assignedDoctorId,
    joinedOn: row.created_at ?? '',
  };
}

function toAppointment(row: any): Appointment {
  return {
    id: row.id,
    patientId: row.patient_id,
    doctorId: row.doctor_id,
    startsAt: row.starts_at,
    durationMinutes: row.duration_minutes,
    status: String(row.status ?? '').toLowerCase() as any,
    reason: row.reason,
    createdAt: row.created_at,
    updatedAt: row.updatedAt ?? row.updated_at,
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

function toSpecialization(row: any): Specialization {
  return {
    id: row.id,
    name: row.name,
    description: row.description ?? undefined,
  };
}

function toMedicalRecord(row: any): MedicalRecord {
  return {
    id: row.id,
    patientId: row.patient_id,
    title: row.title,
    description: row.description ?? undefined,
    createdAt: row.created_at,
  };
}

function toPrescription(row: any): Prescription {
  return {
    id: row.id,
    appointmentId: row.appointment_id,
    medicine: row.medicine,
    dosage: row.dosage,
    frequency: row.frequency,
    prescribedAt: row.prescribed_at,
  };
}

function toInvoice(row: any): Invoice {
  return {
    id: row.id,
    patientId: row.patient_id,
    amount: row.amount,
    status: row.status,
    dueDate: row.due_date,
  };
}

function toNotification(row: any): Notification {
  return {
    id: row.id,
    userId: row.user_id,
    title: row.title,
    body: row.body,
    isRead: row.is_read,
    createdAt: row.created_at,
  };
}

@Injectable({ providedIn: 'root' })
export class HttpPatientGateway {
  private supabase = inject(SupabaseService);

  doctorIdAliases(doctorId: string): Observable<string[]> {
    if (!doctorId) return of([]);
    return from(
      (this.supabase as any)
        .from('doctors')
        .select('id,user_id')
        .or(`id.eq.${doctorId},user_id.eq.${doctorId}`),
    ).pipe(
      map((r: any) => {
        const ids = new Set<string>([String(doctorId)]);
        for (const row of r?.data ?? []) {
          if (row?.id) ids.add(String(row.id));
          if (row?.user_id) ids.add(String(row.user_id));
        }
        return Array.from(ids);
      }),
      catchError(() => of([String(doctorId)])),
    );
  }

  // ---- Profile ----
  getProfile(): Observable<UserProfile> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any)
            .from('profiles')
            .select('*')
            .eq('user_id', result.data.user.id)
            .single(),
        ).pipe(
          map((profileResult: any) => {
            if (profileResult.error || !profileResult.data) {
              throw new Error('Profile not found');
            }
            return toProfile(profileResult.data);
          }),
        );
      }),
    );
  }

  updateProfile(profile: Partial<UserProfile>): Observable<UserProfile> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any)
            .from('profiles')
            .update({
              username: profile.username,
              full_name: profile.fullName,
              email: profile.email,
              status: profile.status,
            })
            .eq('user_id', result.data.user.id)
            .select()
            .single(),
        ).pipe(
          map((r: any) => {
            if (r.error || !r.data) {
              throw new Error(r.error?.message ?? 'Update failed');
            }
            return toProfile(r.data);
          }),
        );
      }),
    );
  }

  // ---- Appointments ----
  getAppointments(filter?: AppointmentFilter): Observable<Paged<Appointment>> {
    let q = (this.supabase as any).from('appointments').select('*', { count: 'exact' });
    if (filter?.doctorId) {
      q = q.eq('doctor_id', filter.doctorId);
    }
    if (filter?.status) {
      q = q.eq('status', filter.status);
    }
    if (filter?.limit) {
      q = q.limit(filter.limit);
    }
    if (filter?.offset) {
      q = q.range(filter.offset, filter.offset + (filter.limit ?? 20) - 1);
    } else if (filter?.limit) {
      q = q.range(0, filter.limit - 1);
    }
    return from(q).pipe(
      map((r: any) => ({
        items: (r.data ?? []).map(toAppointment),
        total: r.count ?? (r.data ?? []).length,
        limit: filter?.limit ?? (r.data ?? []).length,
        offset: filter?.offset ?? 0,
      })),
    );
  }

  getAppointment(id: string): Observable<Appointment> {
    return from((this.supabase as any).from('appointments').select('*').eq('id', id).single()).pipe(
      map((r: any) => {
        if (r.error || !r.data) {
          throw new Error(r.error?.message ?? 'Appointment not found');
        }
        return toAppointment(r.data);
      }),
    );
  }

  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any).rpc('book_appointment', {
            p_doctor_id: request.doctorId,
            p_starts_at: request.startsAt,
            p_duration_minutes: request.durationMinutes,
            p_reason: request.reason,
          }),
        ).pipe(
          map((r: any) => {
            if (r.error) {
              throw new Error(r.error?.message ?? 'Booking failed');
            }
            return {
              id: r.data,
              patientId: '',
              doctorId: request.doctorId,
              startsAt: request.startsAt,
              durationMinutes: request.durationMinutes,
              status: 'BOOKED',
              reason: request.reason,
              createdAt: new Date().toISOString(),
              updatedAt: new Date().toISOString(),
            } as Appointment;
          }),
        );
      }),
    );
  }

  // ---- Doctor directory ----
  getDoctors(params?: {
    specializationId?: string;
    search?: string;
    limit?: number;
    offset?: number;
  }): Observable<Paged<Doctor>> {
    let q = (this.supabase as any).from('doctors').select('*', { count: 'exact' });
    if (params?.specializationId) {
      q = q.eq('specialization_id', params.specializationId);
    }
    if (params?.search) {
      q = q.ilike('full_name', `%${params.search}%`);
    }
    if (params?.limit) {
      q = q.limit(params.limit);
    }
    if (params?.offset) {
      q = q.range(params.offset, params.offset + (params.limit ?? 20) - 1);
    } else if (params?.limit) {
      q = q.range(0, params.limit - 1);
    }
    return from(q).pipe(
      map((r: any) => ({
        items: (r.data ?? []).map(toDoctor),
        total: r.count ?? (r.data ?? []).length,
        limit: params?.limit ?? (r.data ?? []).length,
        offset: params?.offset ?? 0,
      })),
    );
  }

  /**
   * Resolve the signed-in doctor record robustly across ID mappings.
   * Tries direct matches, users/profile bridges, and (as a safe fallback for
   * small demo datasets) scans doctors to match by any linked candidate ID.
   */
  getCurrentDoctor(): Observable<Doctor> {
    return from(this.supabase.getUser()).pipe(
      switchMap((authResult: any) => {
        if (authResult.error || !authResult.data?.user) {
          throw new Error('Not authenticated');
        }
        const authUser = authResult.data.user;
        const authUserId = String(authUser.id ?? '');
        const authEmail = String(authUser.email ?? '').toLowerCase();

        const tryDoctor = (col: 'id' | 'user_id', val: string) =>
          from((this.supabase as any).from('doctors').select('*').eq(col, val).maybeSingle()).pipe(
            map((r: any) => (!r?.error && r?.data ? toDoctor(r.data) : null)),
            catchError(() => of(null)),
          );

        const findInAllDoctors = (candidates: Set<string>, profileName?: string, profileEmail?: string) =>
          from((this.supabase as any).from('doctors').select('*')).pipe(
            map((r: any) => {
              const rows = (r?.data ?? []) as any[];
              const norm = (v: any) => (v == null ? '' : String(v).trim().toLowerCase());
              const cand = new Set(Array.from(candidates).map(norm).filter(Boolean));
              let best: Doctor | null = null;
              for (const row of rows) {
                const rid = norm(row?.id);
                const ruid = norm(row?.user_id);
                if ((rid && cand.has(rid)) || (ruid && cand.has(ruid))) {
                  best = toDoctor(row);
                  break;
                }
              }
              if (!best && profileEmail) {
                // no email column on doctors; try name/email hints only as last resort
              }
              if (!best && profileName) {
                const pn = norm(profileName).replace(/^dr\.?\s*/, '').trim();
                if (pn) {
                  const match = rows.find((row) => {
                    const fn = norm(row?.full_name).replace(/^dr\.?\s*/, '').trim();
                    return fn && (fn === pn || fn.includes(pn) || pn.includes(fn));
                  });
                  if (match) best = toDoctor(match);
                }
              }
              return best;
            }),
            catchError(() => of(null)),
          );

        const profile$ = from(
          (this.supabase as any).from('profiles').select('*').eq('user_id', authUserId).maybeSingle(),
        ).pipe(
          map((r: any) => (!r?.error && r?.data ? r.data : null)),
          catchError(() => of(null)),
        );
        const user$ = from(
          (this.supabase as any)
            .from('users')
            .select('id,auth_user_id,assigned_doctor_id,email,username,full_name')
            .or(`auth_user_id.eq.${authUserId},id.eq.${authUserId}${authEmail ? `,email.eq.${authEmail}` : ''}`)
            .limit(5),
        ).pipe(
          map((r: any) => ((r?.data ?? []) as any[])),
          catchError(() => of([] as any[])),
        );

        return tryDoctor('user_id', authUserId).pipe(
          switchMap((d) => (d ? of(d) : tryDoctor('id', authUserId))),
          switchMap((d) => {
            if (d) return of(d);
            return profile$.pipe(
              switchMap((profileRow) =>
                user$.pipe(
                  switchMap((userRows) => {
                    const candidates = new Set<string>([authUserId]);
                    if (profileRow?.user_id) candidates.add(String(profileRow.user_id));
                    if (profileRow?.id) candidates.add(String(profileRow.id));
                    if (profileRow?.assigned_doctor_id) candidates.add(String(profileRow.assigned_doctor_id));
                    for (const u of userRows) {
                      if (u?.id) candidates.add(String(u.id));
                      if (u?.auth_user_id) candidates.add(String(u.auth_user_id));
                      if (u?.assigned_doctor_id) candidates.add(String(u.assigned_doctor_id));
                    }
                    const candidateList = Array.from(candidates).filter((v) => v && v.trim());
                    const tryCandidates = (idx: number): Observable<Doctor | null> => {
                      if (idx >= candidateList.length) return of(null);
                      const c = candidateList[idx];
                      return tryDoctor('user_id', c).pipe(
                        switchMap((found) => (found ? of(found) : tryDoctor('id', c))),
                        switchMap((found) => (found ? of(found) : tryCandidates(idx + 1))),
                      );
                    };
                    return tryCandidates(0).pipe(
                      switchMap((found) =>
                        found
                          ? of(found)
                          : findInAllDoctors(
                              candidates,
                              profileRow?.full_name ?? profileRow?.fullName ?? authUser?.user_metadata?.full_name,
                              profileRow?.email ?? authEmail,
                            ),
                      ),
                    );
                  }),
                ),
              ),
            );
          }),
          map((doctor) => {
            if (!doctor) throw new Error('Doctor not found');
            return doctor;
          }),
        );
      }),
    );
  }

  getDoctor(id: string): Observable<Doctor> {
    if (!id) {
      return this.getCurrentDoctor();
    }
    return from(
      (this.supabase as any).from('doctors').select('*').or(`user_id.eq.${id},id.eq.${id}`).maybeSingle(),
    ).pipe(
      map((r: any) => (!r?.error && r?.data ? toDoctor(r.data) : null)),
      catchError(() => of(null)),
      switchMap((doctor) => (doctor ? of(doctor) : this.getCurrentDoctor())),
      map((doctor) => {
        if (!doctor) {
          throw new Error('Doctor not found');
        }
        return doctor;
      }),
    );
  }

  getSpecializations(): Observable<Specialization[]> {
    return from((this.supabase as any).from('specializations').select('*')).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to load specializations');
        }
        return (r.data ?? []).map(toSpecialization);
      }),
    );
  }

  // ---- Medical records ----
  getMedicalRecords(): Observable<MedicalRecord[]> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any)
            .from('medical_records')
            .select('*')
            .eq('patient_id', result.data.user.id),
        ).pipe(
          map((r: any) => {
            if (r.error) {
              throw new Error(r.error?.message ?? 'Failed to load records');
            }
            return (r.data ?? []).map(toMedicalRecord);
          }),
        );
      }),
    );
  }

  // ---- Prescriptions ----
  getPrescriptions(): Observable<Prescription[]> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any)
            .from('prescriptions')
            .select('*')
            .eq('patient_id', result.data.user.id),
        ).pipe(
          map((r: any) => {
            if (r.error) {
              throw new Error(r.error?.message ?? 'Failed to load prescriptions');
            }
            return (r.data ?? []).map(toPrescription);
          }),
        );
      }),
    );
  }

  // ---- Invoices / Payments ----
  getInvoices(): Observable<Invoice[]> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any).from('invoices').select('*').eq('patient_id', result.data.user.id),
        ).pipe(
          map((r: any) => {
            if (r.error) {
              throw new Error(r.error?.message ?? 'Failed to load invoices');
            }
            return (r.data ?? []).map(toInvoice);
          }),
        );
      }),
    );
  }

  payInvoice(request: PaymentRequest): Observable<Invoice> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any)
            .from('payments')
            .insert({
              invoice_id: request.invoiceId,
              patient_id: result.data.user.id,
              amount: request.amount,
              method: request.method,
            })
            .select()
            .single(),
        ).pipe(
          map((r: any) => {
            if (r.error || !r.data) {
              throw new Error(r.error?.message ?? 'Payment failed');
            }
            return toInvoice(r.data);
          }),
        );
      }),
    );
  }

  // ---- Notifications ----
  getNotifications(): Observable<Notification[]> {
    return from(this.supabase.getUser()).pipe(
      switchMap((result: any) => {
        if (result.error || !result.data?.user) {
          throw new Error('Not authenticated');
        }
        return from(
          (this.supabase as any)
            .from('notifications')
            .select('*')
            .eq('user_id', result.data.user.id),
        ).pipe(
          map((r: any) => {
            if (r.error) {
              throw new Error(r.error?.message ?? 'Failed to load notifications');
            }
            return (r.data ?? []).map(toNotification);
          }),
        );
      }),
    );
  }

  markNotificationAsRead(id: string): Observable<void> {
    return from(
      (this.supabase as any).from('notifications').update({ is_read: true }).eq('id', id),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to mark as read');
        }
      }),
    );
  }

  // ---- Doctor area: weekly schedule ----
  getDoctorSchedule(doctorId: string): Observable<ScheduleDay[]> {
    return this.doctorIdAliases(doctorId).pipe(
      switchMap((ids) => {
        let q = (this.supabase as any).from('clinic_hours').select('*');
        if (ids.length === 1) q = q.eq('doctor_id', ids[0]);
        else if (ids.length > 1) q = q.in('doctor_id', ids);
        return from(q);
      }),
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to load schedule');
        }
        const byDay = new Map<number, { startTime: string; endTime: string }[]>();
        for (const d of r.data ?? []) {
          if (d.is_closed || !d.start_time || !d.end_time) continue;
          const day = Number(d.day_of_week);
          const sessions = byDay.get(day) ?? [];
          sessions.push({
            startTime: String(d.start_time).slice(0, 5),
            endTime: String(d.end_time).slice(0, 5),
          });
          byDay.set(day, sessions);
        }
        return Array.from(byDay.entries())
          .sort((a, b) => a[0] - b[0])
          .map(([dayOfWeek, sessions]) => ({
            dayOfWeek,
            sessions: sessions.sort((a, b) => a.startTime.localeCompare(b.startTime)),
          }));
      }),
    );
  }

  saveDoctorSchedule(doctorId: string, days: readonly ScheduleDay[]): Observable<void> {
    return this.doctorIdAliases(doctorId).pipe(
      switchMap((ids) =>
        from(
          (async () => {
            const aliases = ids.length ? ids : [doctorId].filter(Boolean);
            let del = (this.supabase as any).from('clinic_hours').delete();
            if (aliases.length <= 1) del = del.eq('doctor_id', aliases[0] ?? doctorId);
            else del = del.in('doctor_id', aliases);
            const delRes = await del;
            if (delRes.error) {
              throw new Error(delRes.error?.message ?? 'Failed to save schedule');
            }

            const rows = days.flatMap((d) =>
              d.sessions.map((s) => ({
                doctor_id: doctorId,
                day_of_week: d.dayOfWeek,
                start_time: s.startTime,
                end_time: s.endTime,
                is_closed: false,
              })),
            );
            if (!rows.length) return;
            const ins = await (this.supabase as any).from('clinic_hours').insert(rows);
            if (ins.error) {
              throw new Error(ins.error?.message ?? 'Failed to save schedule');
            }
          })(),
        ),
      ),
      map(() => undefined),
    );
  }

  // ---- Doctor area: dated availability overrides ----
  getDoctorAvailability(doctorId: string): Observable<DatedAvailability[]> {
    return this.doctorIdAliases(doctorId).pipe(
      switchMap((ids) => {
        let q = (this.supabase as any).from('availabilities').select('*');
        if (ids.length === 1) q = q.eq('doctor_id', ids[0]);
        else if (ids.length > 1) q = q.in('doctor_id', ids);
        return from(q);
      }),
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to load availability');
        }
        return (r.data ?? []).map((d: any) => ({
          date: d.date,
          sessions: d.sessions,
        }));
      }),
    );
  }

  saveDoctorAvailability(doctorId: string, entry: DatedAvailability): Observable<void> {
    return this.doctorIdAliases(doctorId).pipe(
      switchMap((ids) =>
        from(
          (async () => {
            const aliases = ids.length ? ids : [doctorId].filter(Boolean);
            if (!entry.sessions || entry.sessions.length === 0) {
              let del = (this.supabase as any)
                .from('availabilities')
                .delete()
                .eq('date', entry.date);
              if (aliases.length <= 1) del = del.eq('doctor_id', aliases[0] ?? doctorId);
              else del = del.in('doctor_id', aliases);
              const delRes = await del;
              if (delRes.error) throw new Error(delRes.error?.message ?? 'Failed to clear availability');
              return;
            }
            const up = await (this.supabase as any).from('availabilities').upsert({
              doctor_id: doctorId,
              date: entry.date,
              sessions: entry.sessions,
            });
            if (up.error) {
              throw new Error(up.error?.message ?? 'Failed to save availability');
            }
          })(),
        ),
      ),
      map(() => undefined),
    );
  }

  // ---- Doctor area: record visit outcome ----
  recordAppointmentOutcome(appointmentId: string, outcome: VisitOutcome): Observable<Appointment> {
    return from(
      (this.supabase as any).rpc('record_outcome', {
        p_appointment_id: appointmentId,
        p_outcome: outcome,
      }),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to record outcome');
        }
        return {
          id: appointmentId,
          patientId: '',
          doctorId: '',
          startsAt: '',
          durationMinutes: 0,
          status: 'COMPLETED',
          reason: '',
          createdAt: '',
          updatedAt: '',
        } as Appointment;
      }),
    );
  }

  // ---- Doctor area: confirm a booking ----
  confirmAppointment(appointmentId: string): Observable<Appointment> {
    return from(
      (this.supabase as any).rpc('confirm_appointment', {
        p_appointment_id: appointmentId,
      }),
    ).pipe(
      map((r: any) => {
        if (r.error) {
          throw new Error(r.error?.message ?? 'Failed to confirm');
        }
        return {
          id: appointmentId,
          patientId: '',
          doctorId: '',
          startsAt: '',
          durationMinutes: 0,
          status: 'CONFIRMED',
          reason: '',
          createdAt: '',
          updatedAt: '',
        } as Appointment;
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

  /** Send an FCM push notification via the Edge Function. Best-effort. */
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

  // ---- Doctor area: appointments for this doctor ----
  getDoctorAppointments(filter?: AppointmentFilter): Observable<Paged<Appointment>> {
    // RLS (with tolerant doctor policies) scopes rows for the signed-in doctor.
    // Avoid client-side doctor_id filters here because stored ids may use auth uid,
    // users.id, or doctors.id variants.
    const { doctorId: _ignoredDoctorId, ...rest } = filter ?? {};
    return this.getAppointments(rest);
  }

  // ---- Doctor area: patients with appointments ----
  getPatients(doctorId?: string): Observable<UserProfile[]> {
    const mapUsers = (rows: any[]) =>
      (rows ?? []).map((u: any) =>
        toProfile({
          ...u,
          user_id: u.id ?? u.user_id,
          id: u.id ?? u.user_id,
        }),
      );

    return from(
      (this.supabase as any)
        .from('profiles')
        .select('*')
        .in('role', ['PATIENT', 'patient']),
    ).pipe(
      map((r: any) => {
        if (r.error) throw new Error(r.error?.message ?? 'Failed to load patients');
        const rows = r.data ?? [];
        if (!rows.length) throw new Error('No patient profiles');
        return rows.map((p: any) => ({ ...toProfile(p), usersId: p.user_id ?? p.id }));
      }),
      catchError(() =>
        from(
          (this.supabase as any)
            .from('users')
            .select('id, username, email, role, status, auth_user_id, full_name, mobile_number, date_of_birth')
            .in('role', ['PATIENT', 'patient']),
        ).pipe(
          map((r: any) => {
            if (r.error) throw new Error(r.error?.message ?? 'Failed to load patients');
            return mapUsers(r.data ?? []).map((u: any) => ({ ...u, usersId: u.id }));
          }),
        ),
      ),
    );
  }
}
