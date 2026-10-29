import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize, tap } from 'rxjs';
import { AuthSession } from './models';
import { AuthState } from './auth.state';
import { environment } from '../../../environments/environment';
const SIGN_IN = `${environment.apiBaseUrl}/auth/sign-in`;
const REFRESH = `${environment.apiBaseUrl}/auth/refresh`;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private authState = inject(AuthState);
  private refreshInFlight: Observable<AuthSession> | null = null;

  signIn(username: string, password: string): Observable<AuthSession> {
    return this.http
      .post<AuthSession>(SIGN_IN, { username, password })
      .pipe(tap((session) => this.authState.setSession(session)));
  }

  setSessionFromGateway(session: AuthSession): void {
    this.authState.setSession(session);
  }

  /**
   * Refreshes the access token. Concurrent callers share a single in-flight
   * /auth/refresh attempt so a burst of 401s cannot trigger a request storm.
   * When refresh itself fails, stored tokens/session are cleared and the error
   * is propagated so the interceptor can sign out and redirect to login.
   */
  refresh(): Observable<AuthSession> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }
    const refreshToken = this.authState.refreshToken;
    if (!refreshToken) {
      this.authState.clear();
      return throwError(() => new Error('No refresh token'));
    }
    this.refreshInFlight = this.http
      .post<AuthSession>(
        REFRESH,
        {},
        {
          headers: new HttpHeaders().set('Authorization', `Bearer ${refreshToken}`),
        },
      )
      .pipe(
        tap((session) => this.authState.setSession(session)),
        catchError((err) => {
          this.authState.clear();
          return throwError(() => err);
        }),
        finalize(() => {
          this.refreshInFlight = null;
        }),
      );
    return this.refreshInFlight;
  }

  signOut(): void {
    this.authState.clear();
  }

  get accessToken(): string | null {
    return this.authState.accessToken;
  }

  get profile() {
    return this.authState.profile;
  }

  get isAuthenticated$(): Observable<boolean> {
    return this.authState.isAuthenticated;
  }
}
