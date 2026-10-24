import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { StaffLogin } from './auth/staff-login';
import { DoctorDashboard } from './doctor/dashboard';

export const routes: Routes = [
  { path: 'login', component: StaffLogin },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'doctor', component: DoctorDashboard, canActivate: [authGuard] },
  { path: '**', redirectTo: '/login' },
];
