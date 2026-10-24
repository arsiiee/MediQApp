import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile, Appointment, MessageThread } from './models';
import { environment } from '../../../environments/environment';

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
    return this.http.get<MessageThread[]>(`${BASE}/secretary/messages`);
  }
}
