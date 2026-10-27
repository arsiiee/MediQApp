import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { UserProfile, Appointment, MessageThread, Message, Doctor } from './models';
import { environment } from '../../../environments/environment';
import type { Conversation } from '../../secretary/secretary.models';
import type { ConversationMessage } from '../../secretary/secretary.models';

const BASE = `${environment.apiBaseUrl}`;

/** A doctor's published hours for one day, as returned by the schedule endpoint. */
interface ClinicHours {
  readonly dayOfWeek: number;
  readonly startTime: string;
  readonly endTime: string;
  readonly isClosed: boolean;
}

function toConversationMessage(m: Message): ConversationMessage {
  return {
    id: m.id,
    conversationId: m.conversationId,
    sentAt: m.sentAt,
    body: m.body,
    fromSecretary: m.authorRole === 'secretary',
    readAt: m.readAt ?? null,
  };
}

@Injectable({ providedIn: 'root' })
export class HttpSecretaryGateway {
  private http = inject(HttpClient);

  getAppointments(): Observable<Appointment[]> {
    return this.http.get<Appointment[]>(`${BASE}/secretary/appointments`);
  }

  confirmAppointment(id: string): Observable<void> {
    return this.http.post<void>(`${BASE}/secretary/appointments/${id}/confirm`, null);
  }

  cancelAppointment(id: string): Observable<void> {
    return this.http.post<void>(`${BASE}/secretary/appointments/${id}/cancel`, null);
  }

  getPatients(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${BASE}/secretary/patients`);
  }

  getMessages(): Observable<MessageThread[]> {
    return this.http.get<MessageThread[]>(`${BASE}/messages/conversations`);
  }

  getConversation(id: string): Observable<Conversation> {
    return this.http.get<Conversation>(`${BASE}/messages/conversations/${id}`);
  }

  getConversationMessages(conversationId: string): Observable<ConversationMessage[]> {
    return this.http
      .get<Message[]>(`${BASE}/messages/conversations/${conversationId}/messages`)
      .pipe(map((messages) => messages.map(toConversationMessage)));
  }

  /** The secretary's assigned doctor — the desk this screen works. */
  getDoctor(): Observable<Doctor> {
    return this.http.get<Doctor>(`${BASE}/secretary/doctor`);
  }

  /** The assigned doctor's published weekly hours. */
  getDoctorSchedule(doctorId: string): Observable<ClinicHours[]> {
    return this.http.get<ClinicHours[]>(`${BASE}/secretary/doctor/${doctorId}/schedule`);
  }

  sendMessage(
    conversationId: string,
    body: string,
  ): Observable<ConversationMessage> {
    return this.http
      .post<Message>(`${BASE}/messages/conversations/${conversationId}/messages`, body)
      .pipe(map(toConversationMessage));
  }

  markConversationRead(conversationId: string): Observable<void> {
    return this.http.patch<void>(
      `${BASE}/messages/conversations/${conversationId}/read`,
      null,
    );
  }
}
