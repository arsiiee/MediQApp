import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpDoctorGateway } from '../core/api/http-doctor-gateway';
import { AuthService } from '../core/api/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-doctor-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard">
      <header>
        <h1>MediQ &mdash; Doctor Dashboard</h1>
        <div class="user">{{ profile?.fullName }} ({{ profile?.role }})</div>
        <button (click)="signOut()">Sign out</button>
      </header>
      <section>
        <h2>Doctors</h2>
        <ul>
          <li *ngFor="let d of doctors">{{ d.fullName }} &mdash; {{ d.specialty }}</li>
          <li *ngIf="doctors.length === 0">No doctors found.</li>
        </ul>
        <p *ngIf="error" class="error">{{ error }}</p>
      </section>
    </div>
  `,
  styles: [
    `
      .dashboard { max-width: 900px; margin: 24px auto; font-family: system-ui, sans-serif; }
      header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
      h1 { font-size: 1.5rem; margin: 0; }
      .user { color: #555; font-size: 0.95rem; }
      .error { color: #c0392b; font-size: 0.9rem; }
    `,
  ],
})
export class DoctorDashboard implements OnInit {
  private gateway = inject(HttpDoctorGateway);
  private auth = inject(AuthService);
  private router = inject(Router);

  profile = this.auth.profile;
  doctors: any[] = [];
  error: string | null = null;

  ngOnInit(): void {
    this.gateway.getDoctors().subscribe({
      next: (d) => (this.doctors = d.items ?? []),
      error: () => (this.error = 'Failed to load doctors from the API.'),
    });
  }

  signOut(): void {
    this.auth.signOut();
    this.router.navigateByUrl('/login');
  }
}
