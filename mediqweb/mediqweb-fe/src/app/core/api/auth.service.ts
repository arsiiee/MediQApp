import { Injectable, inject } from '@angular/core';
import {
  BehaviorSubject,
  distinctUntilChanged,
  from,
  map,
  of,
  Observable,
  switchMap,
  throwError,
} from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { AuthSession, UserProfile } from './models';
import { AuthState } from './auth.state';
import { SupabaseService } from './supabase.service';

function mapProfile(row: any): UserProfile {
  return {
    id: row.user_id ?? row.id,
    username: row.username ?? '',
    fullName: row.full_name ?? row.fullName ?? '',
    email: row.email ?? '',
    mobileNumber: row.mobile_number ?? row.mobileNumber ?? row.email ?? '',
    dateOfBirth: row.date_of_birth ?? row.dateOfBirth ?? '',
    role: ((row.role ?? 'patient') as string).toLowerCase() as UserProfile['role'],
    status: row.status ?? 'ACTIVE',
    assignedDoctorId: row.assigned_doctor_id ?? row.assignedDoctorId ?? null,
  };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private supabase = inject(SupabaseService);
  private authState = inject(AuthState);

  public profile$ = new BehaviorSubject<UserProfile | null>(null);
  public accessToken$ = new BehaviorSubject<string | null>(null);
  private initialized = new BehaviorSubject<boolean>(false);
  public readonly initialized$ = this.initialized.asObservable();

  constructor() {
    // Restore session/profile on startup. getSession() reads from sessionStorage
    // asynchronously, so we must wait for it to finish before route guards can
    // reliably decide whether the user is authenticated (fixes logout on reload).
    console.log('[AuthService] constructor: calling getSession()');
    from(this.supabase.getSession()).pipe(
      catchError((err) => {
        console.error('[AuthService] getSession() error:', err);
        return of({ data: { session: null } });
      }),
      tap(({ data }) => {
        console.log('[AuthService] getSession() result:', data.session ? `session for user ${data.session.user.id}` : 'no session');
      })
    ).subscribe(({ data }) => {
      const session = data.session;
      this.accessToken$.next(session?.access_token ?? null);
      console.log('[AuthService] accessToken$ set:', !!session?.access_token);
      if (session?.user) {
        console.log('[AuthService] loading profile for user:', session.user.id);
        this.loadProfile(session.user.id, session.user);
      } else {
        this.profile$.next(null);
        this.initialized.next(true);
        console.log('[AuthService] no session, initialized=true');
      }
    });
    // React to auth state changes (e.g. sign-out in another tab).
    this.supabase.onAuthStateChange((event, session) => {
      console.log('[AuthService] onAuthStateChange:', event, session?.user?.id ?? 'no user');
      this.accessToken$.next(session?.access_token ?? null);
      if (event === 'SIGNED_OUT' || !session?.user) {
        // Clear session state but keep initialized=true so guards can make a
        // proper decision instead of hanging waiting for initialized to flip.
        this.authState.clear();
        this.profile$.next(null);
        console.log('[AuthService] signed out');
      } else if (event === 'SIGNED_IN' && session.user) {
        this.loadProfile(session.user.id, session.user);
      }
    });
  }

  private loadProfile(userId: string, user?: any): void {
    console.log('[AuthService] loadProfile() starting for user:', userId);
    from(this.supabase.getProfile(userId))
      .pipe(
        map((r: any) => r.data ? mapProfile(r.data) : null),
        catchError((err) => {
          console.error('[AuthService] loadProfile() error:', err);
          // If the profiles table read fails (e.g. RLS), build a minimal
          // profile from the Supabase session user data so roleGuard still has
          // a role to check instead of redirecting to /login.
          if (user) {
            return of({
              id: user.id,
              username: user.email?.split('@')[0] ?? 'user',
              fullName: user.user_metadata?.full_name ?? user.user_metadata?.fullName ?? '',
              email: user.email ?? '',
              mobileNumber: user.user_metadata?.mobile_number ?? user.user_metadata?.mobileNumber ?? user.email ?? '',
              dateOfBirth: user.user_metadata?.date_of_birth ?? user.user_metadata?.dateOfBirth ?? '',
              role: ((user.user_metadata?.role ?? 'patient') as string).toLowerCase() as UserProfile['role'],
              status: 'ACTIVE',
              assignedDoctorId: undefined,
            } as UserProfile);
          }
          return of(null as UserProfile | null);
        }),
        tap((p) => console.log('[AuthService] loadProfile() result:', p ? `profile for ${p.id}` : 'null profile'))
      )
      .subscribe((p) => {
        this.profile$.next(p);
        // Always set authState.session$ so AuthState.accessToken and
        // AuthState.session are available even when the DB profile read fails.
        this.authState.setSession({
          accessToken: this.accessToken$.value ?? '',
          expiresAt: '',
          refreshToken: '',
          refreshExpiresAt: '',
          profile: p as UserProfile,
        });
        this.initialized.next(true);
        console.log('[AuthService] profile$ set, initialized=true');
      });
  }

  private resolveEmail(emailOrUsername: string): Promise<string> {
    if (emailOrUsername.includes('@')) {
      return Promise.resolve(emailOrUsername);
    }
    return (this.supabase as any)
      .from('profiles')
      .select('email')
      .eq('username', emailOrUsername)
      .single()
      .then((r: any) => {
        if (r.error || !r.data) {
          throw new Error('Invalid credentials');
        }
        return r.data.email;
      });
  }

  signIn(emailOrUsername: string, password: string): Observable<AuthSession> {
    return from(this.resolveEmail(emailOrUsername)).pipe(
      switchMap((email) =>
        from(this.supabase.signIn(email, password)).pipe(
          switchMap((result: any) => {
            if (result.error || !result.data?.user) {
              return throwError(() => new Error(result.error?.message ?? 'Invalid credentials'));
            }
            return from(this.supabase.getProfile(result.data.user.id)).pipe(
              map((profileResult: any) => {
                if (profileResult.error || !profileResult.data) {
                  throw new Error('Profile not found');
                }
                const profile = mapProfile(profileResult.data);
                this.profile$.next(profile);
                const session = result.data.session;
                this.accessToken$.next(session?.access_token ?? null);
                const authSession: AuthSession = {
                  accessToken: session?.access_token ?? '',
                  expiresAt: session?.expires_at ?? '',
                  refreshToken: session?.refresh_token ?? '',
                  refreshExpiresAt: '',
                  profile,
                };
                this.authState.setSession(authSession);
                return authSession;
              }),
            );
          }),
        ),
      ),
      catchError((err) => throwError(() => err)),
    );
  }

  setSessionFromGateway(session: AuthSession): void {
    this.authState.setSession(session);
    this.profile$.next(session.profile);
    this.accessToken$.next(session.accessToken);
  }

  refresh(): Observable<AuthSession> {
    return from(this.supabase.getSession()).pipe(
      map(({ data }: any) => {
        const session = data.session;
        if (!session) {
          throw new Error('No session');
        }
        this.accessToken$.next(session.access_token);
        const profile = this.profile$.value;
        if (!profile) {
          throw new Error('No profile');
        }
        const authSession: AuthSession = {
          accessToken: session.access_token,
          expiresAt: session.expires_at ?? '',
          refreshToken: session.refresh_token,
          refreshExpiresAt: '',
          profile,
        };
        this.authState.setSession(authSession);
        return authSession;
      }),
      catchError((err) => throwError(() => err)),
    );
  }

  signOut(): void {
    this.supabase.signOut().catch(() => {
      // Ignore errors — clear local state regardless.
    });
    this.authState.clear();
    this.profile$.next(null);
    this.accessToken$.next(null);
  }

  get accessToken(): string | null {
    return this.accessToken$.value;
  }

  get profile(): UserProfile | null {
    return this.profile$.value;
  }

  get isAuthenticated$(): Observable<boolean> {
    return this.profile$.pipe(
      map((p) => p != null),
      distinctUntilChanged(),
    );
  }
}
