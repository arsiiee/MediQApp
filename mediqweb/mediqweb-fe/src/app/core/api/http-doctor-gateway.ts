import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Doctor, Paged } from './models';
import { environment } from '../../../environments/environment';

const BASE = `${environment.apiBaseUrl}`;

@Injectable({ providedIn: 'root' })
export class HttpDoctorGateway {
  private http = inject(HttpClient);

  getDoctors(params?: { specializationId?: string; search?: string; limit?: number; offset?: number }): Observable<Paged<Doctor>> {
    return this.http.get<Paged<Doctor>>(`${BASE}/doctors`, { params: params as unknown as Record<string, string> });
  }

  getDoctor(id: string): Observable<Doctor> {
    return this.http.get<Doctor>(`${BASE}/doctors/${id}`);
  }
}
