import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import type { StaffAccount, StaffDraft, AuditEntry } from '../../admin/admin.models';
import type { PatientAccount } from '../../admin/admin.models';
import type { AccountStatus } from '../../shared/domain/account-status';
import { environment } from '../../../environments/environment';

const BASE = `${environment.apiBaseUrl}`;

interface AdminAccountsResponse {
  readonly patients: StaffAccount[];
  readonly doctors: StaffAccount[];
  readonly secretaries: StaffAccount[];
}

interface AccountStatusUpdate {
  readonly status: AccountStatus;
}

interface DoctorWithSpecialization {
  readonly id: string;
  readonly userId: string;
  readonly fullName: string;
  readonly specializationId: string | null;
  /** Doctors only — the PRC license number from the doctors table. */
  readonly licenseNumber: string | null;
}

@Injectable({ providedIn: 'root' })
export class HttpAdminGateway {
  private http = inject(HttpClient);

  getAccounts(): Observable<AdminAccountsResponse> {
    return this.http.get<AdminAccountsResponse>(`${BASE}/admin/accounts`);
  }

  createStaffAccount(kind: 'doctor' | 'secretary', draft: StaffDraft): Observable<StaffAccount> {
    return this.http.post<StaffAccount>(`${BASE}/admin/accounts?kind=${kind}`, draft);
  }

  updateAccountStatus(id: string, status: AccountStatus): Observable<StaffAccount> {
    return this.http.patch<StaffAccount>(`${BASE}/admin/accounts/${id}/status`, {
      status,
    });
  }

  updateAssignedDoctor(id: string, doctorId: string): Observable<StaffAccount> {
    return this.http.patch<StaffAccount>(`${BASE}/admin/accounts/${id}/doctor`, {
      assignedDoctorId: doctorId,
    });
  }

  getDoctorsForAdmin(): Observable<StaffAccount[]> {
    return this.http.get<StaffAccount[]>(`${BASE}/admin/doctors`);
  }

  /** Doctor id -> {specializationId, licenseNumber}, from the doctors table. */
  getDoctorSpecializations(): Observable<
    Map<string, { specializationId: string | null; licenseNumber: string | null }>
  > {
    return this.http
      .get<DoctorWithSpecialization[]>(`${BASE}/admin/doctors`)
      .pipe(
        map(
          (doctors) =>
            new Map(
              doctors.map((d) => [
                d.id,
                { specializationId: d.specializationId, licenseNumber: d.licenseNumber },
              ]),
            ),
        ),
      );
  }

  getAuditLogs(): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>(`${BASE}/admin/audit-logs`);
  }
}
