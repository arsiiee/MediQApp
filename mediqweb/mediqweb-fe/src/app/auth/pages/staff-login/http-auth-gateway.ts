import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthSession } from '../../../core/api/models';
import { HttpClient } from '@angular/common/http';
import { StaffCredentials, StaffRole } from './auth.gateway';
import { AuthService } from '../../../core/api/auth.service';
import { environment } from '../../../../environments/environment';

const SIGN_IN = `${environment.apiBaseUrl}/auth/sign-in`;

/**
 * HTTP implementation of AuthGateway for staff sign-in.
 *
 * Calls the Hono BFF (which proxies to Ktor), stores the resulting session,
 * and returns the user's role for routing.
 */
@Injectable()
export class HttpAuthGateway {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  async signIn(credentials: StaffCredentials): Promise<StaffRole> {
    const session = await firstValueFrom(
      this.http.post<{ accessToken: string; refreshToken: string; profile: { role: string } }>(
        SIGN_IN,
        { username: credentials.identifier, password: credentials.password },
      ),
    );
    this.authService.setSessionFromGateway({
      ...session,
      expiresAt: new Date(Date.now() + 3600000).toISOString(),
      refreshExpiresAt: new Date(Date.now() + 86400000).toISOString(),
    } as AuthSession);
    return session.profile.role as StaffRole;
  }
}
