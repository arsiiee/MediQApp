import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/pages/staff-login/staff-login').then((m) => m.StaffLogin),
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'doctor',
    loadChildren: () => import('./doctor/doctor.routes').then((m) => m.DOCTOR_ROUTES),
  },
  {
    path: 'secretary',
    loadChildren: () => import('./secretary/secretary.routes').then((m) => m.SECRETARY_ROUTES),
  },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];
