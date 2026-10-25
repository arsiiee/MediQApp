import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile, Appointment, MessageThread } from './models';
import { environment } from '../../../environments/environment';
import type { Conversation } from '../../secretary/secretary.models';
import type { ConversationMessage } from '../../secretary/secretary.models';

const BASE = `${environment.apiBaseUrl}`;

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
    return this.http.get<ConversationMessage[]>(
      `${BASE}/messages/conversations/${conversationId}/messages`,
    );
  }

  sendMessage(
    conversationId: string,
    body: string,
  ): Observable<ConversationMessage> {
    return this.http.post<ConversationMessage>(
      `${BASE}/messages/conversations/${conversationId}/messages`,
      body,
    );
  }

  markConversationRead(conversationId: string): Observable<void> {
    return this.http.patch<void>(
      `${BASE}/messages/conversations/${conversationId}/read`,
      null,
    );
  }
}
