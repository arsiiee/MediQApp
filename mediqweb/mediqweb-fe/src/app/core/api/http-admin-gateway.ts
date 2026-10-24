import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserProfile } from './models';
import { environment } from '../../../environments/environment';

const BASE = `${environment.apiBaseUrl}`;

@Injectable({ providedIn: 'root' })
export class HttpAdminGateway {
  private http = inject(HttpClient);

  getAccounts(): Observable<any> {
    return this.http.get(`${BASE}/admin/accounts`);
  }
}
