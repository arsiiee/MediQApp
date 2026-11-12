import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './api/auth.service';
import { filter, map, take, tap } from 'rxjs/operators';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  console.log('[AuthGuard] checking, accessToken:', !!auth.accessToken, 'profile:', !!auth.profile);
  // Always wait for session + profile restoration before deciding. getSession()
  // reads sessionStorage asynchronously, and loadProfile() fetches the profile
  // from the API. Returning early on accessToken alone lets the route activate
  // before the profile is loaded, so the dashboard sees assignedDoctorId: null.
  return auth.initialized$.pipe(
    filter((initialized) => initialized),
    take(1),
    tap((initialized) => console.log('[AuthGuard] initialized:', initialized, 'accessToken:', !!auth.accessToken)),
    map(() => {
      const result = auth.accessToken ? true : router.createUrlTree(['/login']);
      console.log('[AuthGuard] decision:', result === true ? 'ALLOW' : 'REDIRECT_TO_LOGIN');
      return result;
    })
  );
};
