import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
import { environment } from '../../../environments/environment';
import type { ScheduleDay } from '../../doctor/doctor.models';
import type { DatedAvailability } from '../../doctor/doctor.models';
import type { VisitOutcome } from '../../doctor/doctor.models';

const BASE = `${environment.apiBaseUrl}`;

/**
 * Patient gateway: appointments, doctor directory, prescriptions,
 * medical history, invoices/payments and notifications. Aligns with
 * the Figma patient dashboard (upcoming appointments, find-a-doctor,
 * prescription list, medical records, billing, notifications).
 *
 * URLs match Ktor routes directly (no `/patient/` prefix).
 */
@Injectable({ providedIn: 'root' })
export class HttpPatientGateway {
  private http = inject(HttpClient);

  // ---- Profile ----
  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${BASE}/profile`);
  }

  updateProfile(profile: Partial<UserProfile>): Observable<UserProfile> {
    return this.http.patch<UserProfile>(`${BASE}/profile`, profile);
  }

  // ---- Appointments ----
  getAppointments(filter?: AppointmentFilter): Observable<Paged<Appointment>> {
    return this.http.get<Paged<Appointment>>(`${BASE}/appointments`, {
      params: filter as unknown as Record<string, string>,
    });
  }

  getAppointment(id: string): Observable<Appointment> {
    return this.http.get<Appointment>(`${BASE}/appointments/${id}`);
  }

  createAppointment(request: CreateAppointmentRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${BASE}/appointments`, request);
  }

  // ---- Doctor directory ----
  getDoctors(params?: { specializationId?: string; search?: string; limit?: number; offset?: number }): Observable<Paged<Doctor>> {
    return this.http.get<Paged<Doctor>>(`${BASE}/doctors`, {
      params: params as unknown as Record<string, string>,
    });
  }

  getDoctor(id: string): Observable<Doctor> {
    return this.http.get<Doctor>(`${BASE}/doctors/${id}`);
  }

  getSpecializations(): Observable<Specialization[]> {
    return this.http.get<Specialization[]>(`${BASE}/specializations`);
  }

  // ---- Medical records ----
  getMedicalRecords(): Observable<MedicalRecord[]> {
    return this.http.get<MedicalRecord[]>(`${BASE}/patients/me/records`);
  }

  // ---- Prescriptions ----
  getPrescriptions(): Observable<Prescription[]> {
    return this.http.get<Prescription[]>(`${BASE}/patients/me/prescriptions`);
  }

  // ---- Invoices / Payments ----
  getInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${BASE}/patients/me/invoices`);
  }

  payInvoice(request: PaymentRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${BASE}/patients/me/payments`, request);
  }

  // ---- Notifications ----
  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${BASE}/notifications`);
  }

  markNotificationAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`${BASE}/notifications/${id}/read`, null);
  }

  // ---- Doctor area: weekly schedule ----
  getDoctorSchedule(doctorId: string): Observable<ScheduleDay[]> {
    return this.http.get<ScheduleDay[]>(`${BASE}/doctors/${doctorId}/schedule`);
  }

  saveDoctorSchedule(
    doctorId: string,
    days: readonly ScheduleDay[],
  ): Observable<void> {
    return this.http.post<void>(`${BASE}/doctors/${doctorId}/schedule`, days);
  }

  // ---- Doctor area: dated availability overrides ----
  getDoctorAvailability(doctorId: string): Observable<DatedAvailability[]> {
    return this.http.get<DatedAvailability[]>(
      `${BASE}/doctors/${doctorId}/availability`,
    );
  }

  saveDoctorAvailability(
    doctorId: string,
    entry: DatedAvailability,
  ): Observable<void> {
    return this.http.post<void>(`${BASE}/doctors/${doctorId}/availability`, entry);
  }

  // ---- Doctor area: record visit outcome ----
  recordAppointmentOutcome(
    appointmentId: string,
    outcome: VisitOutcome,
  ): Observable<Appointment> {
    return this.http.patch<Appointment>(
      `${BASE}/doctors/appointments/${appointmentId}/outcome`,
      { outcome },
    );
  }

  // ---- Doctor area: confirm a booking ----
  confirmAppointment(appointmentId: string): Observable<Appointment> {
    return this.http.post<Appointment>(
      `${BASE}/appointments/${appointmentId}/confirm`,
      null,
    );
  }

  // ---- Doctor area: appointments for this doctor ----
  getDoctorAppointments(
    filter?: AppointmentFilter,
  ): Observable<Paged<Appointment>> {
    return this.http.get<Paged<Appointment>>(`${BASE}/appointments`, {
      params: filter as unknown as Record<string, string>,
    });
  }

  // ---- Doctor area: patients with appointments ----
  getPatients(doctorId?: string): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${BASE}/patients`, {
      params: doctorId ? { doctorId } : {},
    });
  }
}
