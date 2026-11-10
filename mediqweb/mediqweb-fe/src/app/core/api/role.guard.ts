import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import type { UserRole } from './models';

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
    if (auth.profile?.role === allowedRole) {
      return true;
    }
    return router.createUrlTree(['/login']);
  };
