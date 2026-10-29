import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  AUTH_GATEWAY,
  type AuthGateway,
  type StaffCredentials,
  type StaffRole,
} from './auth.gateway';
import { HttpAuthGateway } from '../../../core/api/http-auth-gateway';

/**
 * Adapts the real HTTP auth gateway to the `AuthGateway` contract the login
 * page expects. Returns the staff role parsed from the profile so routing to
 * `/admin | /doctor | /secretary` works unchanged.
 */
@Injectable()
export class HttpAuthGatewayAdapter implements AuthGateway {
  private readonly http = inject(HttpAuthGateway);

  async signIn(credentials: StaffCredentials): Promise<StaffRole> {
    const session = await firstValueFrom(
      this.http.signIn(credentials.identifier, credentials.password),
    );
    const role = (session.profile as { role?: string })?.role;
    if (!role || !['admin', 'doctor', 'secretary'].includes(role)) {
      throw new Error('Invalid credentials or the auth service is unreachable.');
    }
    return role as StaffRole;
  }
}

export const provideRealAuthGateway = () => ({
  provide: AUTH_GATEWAY,
  useClass: HttpAuthGatewayAdapter,
});
