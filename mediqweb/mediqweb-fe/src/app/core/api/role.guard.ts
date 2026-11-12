import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import type { UserRole } from './models';
import { map, take } from 'rxjs/operators';

/**
 * Factory for role-based route guards.
 *
 * `roleGuard('admin')` returns a `CanActivateFn` that only admits the signed-in
 * user whose profile role matches `allowedRole`, redirecting everyone else to
 * login. Pair it with `authGuard` (which requires a session) so unauthenticated
 * users land on login before the role check runs.
 *
 * Supports all four roles: patient, doctor, secretary, admin.
 */
export const roleGuard =
  (allowedRole: UserRole): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return auth.initialized$.pipe(
      take(1),
      map(() => {
        const profile = auth.profile;
        console.log('[RoleGuard] allowedRole:', allowedRole, 'profile:', profile?.id ?? 'null', 'profile?.role:', profile?.role ?? 'null');
        if (profile?.role === allowedRole) {
          console.log('[RoleGuard] ALLOW');
          return true;
        }
        console.log('[RoleGuard] REDIRECT_TO_LOGIN');
        return router.createUrlTree(['/login']);
      })
    );
  };
