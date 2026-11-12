import { Injectable, computed, signal, inject } from '@angular/core';
import type { AccountStatus } from '../shared/domain/account-status';
import { HttpPatientGateway } from '../core/api/http-patient-gateway';
import type { UserProfile } from '../core/api/models';
import {
  blockMinutes,
  dayKey,
  isSameDay,
  minutesOfDay,
  parseDayKey,
  startOfDay,
  usableBlocks,
} from './doctor.dates';
import { catchError, map, of, switchMap } from 'rxjs';
import type {
  Appointment,
  AppointmentStatus,
  DatedAvailability,
  DoctorProfile,
  Patient,
  PatientSummary,
  ProfileDraft,
  ScheduleBlock,
  ScheduleDay,
  VisitOutcome,
} from './doctor.models';

export type DatedAvailabilityRefusal =
  'invalid-date' | 'in-the-past' | 'bad-session' | 'overlapping-sessions';

const CLOSED_STATUSES: readonly AppointmentStatus[] = ['completed', 'cancelled', 'no-show'];
const LIVE_STATUSES: readonly AppointmentStatus[] = ['booked', 'confirmed'];

const byStart = (a: Appointment, b: Appointment): number => a.startsAt.localeCompare(b.startsAt);
const byStartNewest = (a: Appointment, b: Appointment): number => byStart(b, a);

function sumMinutes(blocks: readonly ScheduleBlock[]): number {
  return blocks.reduce((total, block) => total + (blockMinutes(block) ?? 0), 0);
}

function sessionsOverlap(sessions: readonly ScheduleBlock[]): boolean {
  return sessions.some((block, i) =>
    sessions.slice(i + 1).some((other) => {
      const aStart = minutesOfDay(block.startTime)!;
      const aEnd = minutesOfDay(block.endTime)!;
      const bStart = minutesOfDay(other.startTime)!;
      const bEnd = minutesOfDay(other.endTime)!;
      return aStart < bEnd && bStart < aEnd;
    }),
  );
}

const EMPTY_PROFILE: DoctorProfile = {
  id: '',
  doctorId: '',
  name: '',
  email: '',
  specialization: '',
  licenseNumber: '',
  joinedOn: '',
  bio: '',
};

function normId(value: string | null | undefined): string | null {
  if (value == null) return null;
  const s = String(value).trim();
  return s ? s.toLowerCase() : null;
}

@Injectable()
export class DoctorSession {
  private readonly gateway = inject(HttpPatientGateway);

  private readonly profileState = signal<DoctorProfile>({ ...EMPTY_PROFILE, doctorId: '' });
  private readonly appointmentState = signal<Appointment[]>([]);
  private readonly patientState = signal<Patient[]>([]);
  private readonly scheduleState = signal<ScheduleDay[]>([]);
  private readonly datedAvailabilityState = signal<DatedAvailability[]>([]);
  private readonly doctorRefIdsState = signal<string[]>([]);
  private readonly knownDoctorRowsState = signal<{ id?: string | null; userId?: string | null }[]>([]);
  private readonly identityResolvedState = signal(false);

  readonly now = signal(new Date());

  readonly doctorId = computed(() => this.profileState().doctorId ?? null);
  readonly doctorUsersId = computed(() => this.profileState().id ?? null);
  readonly profile = computed(() => this.profileState());

  private addDoctorRefs(...values: Array<string | null | undefined>): void {
    const merged = new Set(
      [...this.doctorRefIdsState(), ...values]
        .map((v) => (v == null ? '' : String(v).trim()))
        .filter((v) => v.length > 0),
    );
    this.doctorRefIdsState.set(Array.from(merged));
  }

  private isOwnDoctorAppointment(appointment: Appointment): boolean {
    const refs = new Set(this.doctorRefIdsState().map(normId).filter(Boolean) as string[]);
    const apptDoctor = normId((appointment as any).doctorId);
    if (!apptDoctor) return false;

    // Before doctor identity is resolved, avoid dropping RLS-returned rows.
    // (Hosted data uses mixed doctor ID forms; secretary sees via desk scoping.)
    if (!this.identityResolvedState() || refs.size === 0) return true;

    if (refs.has(apptDoctor)) return true;

    // Secretary-style fallback: match if any known doctor record bridges the IDs.
    for (const d of this.knownDoctorRowsState()) {
      const did = normId(d.id);
      const duid = normId(d.userId);
      const recordIds = [did, duid].filter(Boolean) as string[];
      if (!recordIds.length) continue;
      const refsHitDoctor = recordIds.some((x) => refs.has(x));
      const apptHitsDoctor = recordIds.includes(apptDoctor);
      if (refsHitDoctor && apptHitsDoctor) return true;
    }

    // Last resort: accept if the appointment's doctor_id is a known doctor record
    // and we have not yet established a conflicting doctor identity beyond refs.
    // This keeps single-doctor dashboard usable when linkage is partially broken.
    for (const d of this.knownDoctorRowsState()) {
      const recordIds = [normId(d.id), normId(d.userId)].filter(Boolean) as string[];
      if (recordIds.includes(apptDoctor)) return true;
    }
    return false;
  }

  readonly appointments = computed(() =>
    this.appointmentState()
      .filter((a) => !this.identityResolvedState() || this.isOwnDoctorAppointment(a))
      .sort(byStart),
  );

  readonly openAppointments = computed(() =>
    this.appointments().filter((appointment) => LIVE_STATUSES.includes(appointment.status)),
  );

  readonly todaysAppointments = computed(() => {
    const now = this.now();
    return this.openAppointments().filter((appointment) =>
      isSameDay(new Date(appointment.startsAt), now),
    );
  });

  readonly visitHistory = computed(() =>
    this.appointments()
      .filter(
        (appointment) => appointment.status === 'completed' || appointment.status === 'no-show',
      )
      .sort(byStartNewest),
  );

  readonly nextAppointment = computed(() => {
    const now = this.now().getTime();
    return (
      this.appointments().find(
        (appointment) =>
          LIVE_STATUSES.includes(appointment.status) &&
          new Date(appointment.startsAt).getTime() >= now,
      ) ?? null
    );
  });

  readonly awaitingConfirmation = computed(() =>
    this.appointments().filter((appointment) => appointment.status === 'booked'),
  );

  readonly completedThisWeek = computed(() => {
    const since = startOfDay(this.now()).getTime() - 6 * 24 * 60 * 60 * 1000;
    return this.appointments().filter(
      (appointment) =>
        appointment.status === 'completed' && new Date(appointment.startsAt).getTime() >= since,
    );
  });

  readonly patients = computed<PatientSummary[]>(() => {
    const mine = this.appointments();
    const now = this.now().getTime();

    return this.patientState()
      .map((patient): PatientSummary | null => {
        const pid = normId(patient.id);
        const puid = normId((patient as any).usersId);
        const history = mine
          .filter((appointment) => {
            const ap = normId(appointment.patientId);
            return !!ap && (ap === pid || (puid && ap === puid));
          })
          .sort(byStart);
        if (!history.length) return null;

        const past = history.filter(
          (appointment) =>
            appointment.status !== 'cancelled' && new Date(appointment.startsAt).getTime() < now,
        );
        const future = history.filter(
          (appointment) =>
            new Date(appointment.startsAt).getTime() >= now &&
            !CLOSED_STATUSES.includes(appointment.status),
        );

        return {
          patient,
          lastVisit: past.length ? past[past.length - 1] : null,
          nextVisit: future.length ? future[0] : null,
          visitCount: history.filter(
            (appointment) => appointment.status === 'completed' || appointment.status === 'no-show',
          ).length,
        };
      })
      .filter((summary): summary is PatientSummary => summary !== null);
  });

  readonly schedule = computed(() =>
    [...this.scheduleState()].sort((a, b) => a.dayOfWeek - b.dayOfWeek),
  );

  readonly workingDayCount = computed(
    () => this.scheduleState().filter((day) => day.sessions.length > 0).length,
  );

  readonly weeklyMinutes = computed(() =>
    this.scheduleState().reduce((total, day) => total + sumMinutes(day.sessions), 0),
  );

  readonly datedAvailability = computed(() =>
    [...this.datedAvailabilityState()].sort((a, b) => a.date.localeCompare(b.date)),
  );

  patientById(id: string): Patient | null {
    const needle = normId(id);
    if (!needle) return null;
    return (
      this.patientState().find(
        (patient) => normId(patient.id) === needle || normId((patient as any).usersId) === needle,
      ) ?? null
    );
  }

  patientForDoctor(id: string): Patient | null {
    if (!id) return null;
    const needle = normId(id);
    const known = this.appointments().some((appointment) => normId(appointment.patientId) === needle);
    return known ? this.patientById(id) : null;
  }

  appointmentsForPatient(patientId: string): Appointment[] {
    const needle = normId(patientId);
    return this.appointments().filter(
      (appointment) => normId(appointment.patientId) === needle && appointment.status !== 'cancelled',
    );
  }

  patientName(patientId: string): string {
    return this.patientById(patientId)?.name ?? 'Unknown patient';
  }

  recordOutcome(id: string, outcome: VisitOutcome): boolean {
    const current = this.appointmentState().find(
      (appointment) => appointment.id === id && this.isOwnDoctorAppointment(appointment),
    );
    if (!current || current.status !== 'confirmed') return false;

    this.appointmentState.update((appointments) =>
      appointments.map((appointment) =>
        appointment.id === id ? { ...appointment, status: outcome } : appointment,
      ),
    );

    this.gateway.recordAppointmentOutcome(id, outcome).subscribe({
      error: () => {
        this.appointmentState.update((appointments) =>
          appointments.map((appointment) =>
            appointment.id === id ? { ...appointment, status: 'confirmed' } : appointment,
          ),
        );
      },
    });
    return true;
  }

  confirmAppointment(id: string): boolean {
    const current = this.appointmentState().find(
      (appointment) => appointment.id === id && this.isOwnDoctorAppointment(appointment),
    );
    if (!current || current.status !== 'booked') return false;

    this.appointmentState.update((appointments) =>
      appointments.map((appointment) =>
        appointment.id === id ? { ...appointment, status: 'confirmed' } : appointment,
      ),
    );

    this.gateway.confirmAppointment(id).subscribe({
      next: () => {
        // Best-effort side effects: in-app notification and push. These must not
        // change the confirm result; delivery depends on RPC/RLS/token availability.
        const patient = this.patientById(current.patientId);
        const patientUserId = String(
          (patient as any)?.usersId ?? (patient as any)?.id ?? current.patientId ?? '',
        ).trim();
        const doctorName = String(this.profileState().name ?? '').trim() || 'your doctor';
        const doctorDisplay =
          doctorName === 'your doctor' || /^Dr\.?/i.test(doctorName)
            ? doctorName
            : `Dr. ${doctorName}`;
        if (!patientUserId) return;

        this.gateway.sendNotification(
          patientUserId,
          'CONFIRMED',
          'Appointment confirmed',
          `Your appointment with ${doctorDisplay} has been confirmed.`,
          id,
        ).subscribe({ error: () => {} });

        this.gateway.getPatientFcmToken(patientUserId).subscribe({
          next: (fcmToken) => {
            if (fcmToken) {
              this.gateway
                .sendPushNotification(
                  fcmToken,
                  'Appointment confirmed',
                  `Your appointment with ${doctorDisplay} has been confirmed.`,
                  'mediq://appointments',
                  id,
                )
                .subscribe({ error: () => {} });
            }
          },
          error: () => {},
        });
      },
      error: () => {
        this.appointmentState.update((appointments) =>
          appointments.map((appointment) =>
            appointment.id === id ? { ...appointment, status: 'booked' } : appointment,
          ),
        );
      },
    });
    return true;
  }

  saveSchedule(days: readonly ScheduleDay[]): void {
    this.scheduleState.set(
      days.map((day) => ({ dayOfWeek: day.dayOfWeek, sessions: usableBlocks(day.sessions) })),
    );

    this.gateway.saveDoctorSchedule(this.doctorId() ?? '', this.scheduleState()).subscribe({
      error: () => {
        this.gateway.getDoctorSchedule(this.doctorId() ?? '').subscribe({
          next: (nextDays) => this.scheduleState.set(nextDays),
          error: () => {},
        });
      },
    });
  }

  sessionsOn(date: Date): readonly ScheduleBlock[] {
    const key = dayKey(date);
    const override = this.datedAvailabilityState().find((entry) => entry.date === key);
    const weekly = this.scheduleState().find((day) => day.dayOfWeek === date.getDay());
    return usableBlocks(override ? override.sessions : (weekly?.sessions ?? []));
  }

  fitsInSession(date: Date, startMinutes: number, durationMinutes: number): boolean {
    const endMinutes = startMinutes + durationMinutes;
    return this.sessionsOn(date).some((block) => {
      const open = minutesOfDay(block.startTime);
      const close = minutesOfDay(block.endTime);
      if (open === null || close === null) return false;
      return startMinutes >= open && endMinutes <= close;
    });
  }

  saveDatedAvailability(entry: DatedAvailability): DatedAvailabilityRefusal | null {
    const date = parseDayKey(entry.date);
    if (!date) return 'invalid-date';
    if (date.getTime() < startOfDay(this.now()).getTime()) return 'in-the-past';

    const sessions = usableBlocks(entry.sessions);
    if (sessions.length !== entry.sessions.length) return 'bad-session';
    if (sessionsOverlap(sessions)) return 'overlapping-sessions';

    const key = dayKey(date);
    this.datedAvailabilityState.update((entries) => [
      ...entries.filter((existing) => existing.date !== key),
      { date: key, sessions },
    ]);

    this.gateway.saveDoctorAvailability(this.doctorId() ?? '', entry).subscribe({
      error: () => {
        this.datedAvailabilityState.update((entries) => entries.filter((e) => e.date !== key));
      },
    });
    return null;
  }

  clearDatedAvailability(date: string): boolean {
    const before = this.datedAvailabilityState().length;
    this.datedAvailabilityState.update((entries) => entries.filter((e) => e.date !== date));

    this.gateway
      .saveDoctorAvailability(this.doctorId() ?? '', { date, sessions: [] })
      .subscribe({
        error: () => {
          this.gateway.getDoctorAvailability(this.doctorId() ?? '').subscribe({
            next: (entries) => this.datedAvailabilityState.set(entries),
            error: () => {},
          });
        },
      });
    return this.datedAvailabilityState().length !== before;
  }

  updateProfile(draft: ProfileDraft): void {
    this.profileState.update((profile) => {
      if (!profile) return profile;
      return {
        ...profile,
        name: draft.name,
        email: draft.email,
        bio: draft.bio,
      };
    });

    this.gateway.updateProfile(draft as any).subscribe({
      error: () => {},
    });
  }

  constructor() {
    const resolveDoctorAliases = (seeds: Array<string | null | undefined>) => {
      const uniq = Array.from(
        new Set(seeds.map((s) => (s == null ? '' : String(s).trim())).filter(Boolean)),
      );
      for (const seed of uniq) {
        (this.gateway as any).doctorIdAliases?.(seed)?.subscribe({
          next: (ids: string[]) => this.addDoctorRefs(seed, ...(ids ?? [])),
          error: () => {},
        });
      }
      this.gateway.getDoctors({ limit: 200 }).subscribe({
        next: (paged: any) => {
          const items = (paged?.items ?? []) as Array<{ id?: string; userId?: string }>;
          this.knownDoctorRowsState.set(items.map((d) => ({ id: d?.id ?? null, userId: d?.userId ?? null })));
          const refSet = new Set(this.doctorRefIdsState().map(normId).filter(Boolean) as string[]);
          for (const d of items) {
            const ids = [d?.id, d?.userId].filter(Boolean).map((v) => String(v));
            if (ids.some((id) => refSet.has(normId(id) as string))) this.addDoctorRefs(...ids);
          }
          this.identityResolvedState.set(true);
        },
        error: () => this.identityResolvedState.set(true),
      });
      setTimeout(() => this.identityResolvedState.set(true), 2500);
    };

    this.gateway
      .getProfile()
      .pipe(
        switchMap((profile) =>
          this.gateway.getDoctor(profile.id).pipe(
            map((doctor) => ({ profile, doctor: doctor as any })),
            catchError(() => of({ profile, doctor: null as any })),
          ),
        ),
        catchError(() => of(null as any)),
      )
      .subscribe({
        next: (resolved) => {
          if (!resolved) {
            this.identityResolvedState.set(true);
            return;
          }
          const { profile, doctor } = resolved;
          const refs = [profile.id, doctor?.id, doctor?.userId].filter(Boolean) as string[];
          this.doctorRefIdsState.set(refs);
          this.profileState.set({
            id: profile.id,
            doctorId: doctor?.id ?? '',
            name: profile.fullName || doctor?.fullName || '',
            email: profile.email ?? '',
            specialization: doctor?.specialty ?? '',
            licenseNumber: doctor?.licenseNumber ?? '',
            joinedOn: profile.joinedOn ?? '',
            bio: doctor?.bio ?? '',
          });
          const scheduleId = doctor?.id || doctor?.userId || profile.id;
          if (scheduleId) {
            this.gateway.getDoctorSchedule(scheduleId).subscribe({ next: (days) => this.scheduleState.set(days), error: () => {} });
            this.gateway.getDoctorAvailability(scheduleId).subscribe({ next: (entries) => this.datedAvailabilityState.set(entries), error: () => {} });
          }
          resolveDoctorAliases(refs.length ? refs : [profile.id]);
          if (!doctor) {
            // still try to resolve doctor identity even if initial lookup failed
            this.gateway.getDoctor(profile.id).subscribe({
              next: (d2: any) => {
                if (d2) this.addDoctorRefs(profile.id, d2.id, d2.userId);
              },
              error: () => {},
            });
          }
        },
        error: () => {
          this.gateway.getProfile().subscribe({
            next: (profile) => {
              this.doctorRefIdsState.set([profile.id]);
              this.profileState.set({
                id: profile.id,
                doctorId: '',
                name: profile.fullName,
                email: profile.email ?? '',
                specialization: '',
                licenseNumber: '',
                joinedOn: profile.joinedOn ?? '',
                bio: '',
              });
              this.gateway.getDoctor(profile.id).subscribe({
                next: (doctor: any) => {
                  if (!doctor) {
                    resolveDoctorAliases([profile.id]);
                    return;
                  }
                  this.addDoctorRefs(profile.id, doctor.id, doctor.userId);
                  this.profileState.update((p) => ({
                    ...p,
                    doctorId: doctor.id ?? p.doctorId,
                    specialization: doctor.specialty ?? p.specialization,
                    licenseNumber: doctor.licenseNumber ?? p.licenseNumber,
                    bio: doctor.bio ?? p.bio,
                  }));
                  const scheduleId = doctor.id || doctor.userId || profile.id;
                  this.gateway.getDoctorSchedule(scheduleId).subscribe({ next: (days) => this.scheduleState.set(days), error: () => {} });
                  this.gateway.getDoctorAvailability(scheduleId).subscribe({ next: (entries) => this.datedAvailabilityState.set(entries), error: () => {} });
                  resolveDoctorAliases([profile.id, doctor.id, doctor.userId].filter(Boolean));
                },
                error: () => resolveDoctorAliases([profile.id]),
              });
            },
            error: () => this.identityResolvedState.set(true),
          });
        },
      });

    this.gateway.getDoctorAppointments().subscribe({
      next: (paged) => this.appointmentState.set((paged.items ?? []) as unknown as Appointment[]),
      error: () => {},
    });

    this.gateway.getPatients().subscribe({
      next: (profiles: UserProfile[]) =>
        this.patientState.set(
          profiles.map((p: any) => ({
            id: p.id,
            usersId: p.usersId ?? p.id,
            name: p.fullName ?? p.name ?? '',
            email: p.email ?? '',
            phone: p.mobileNumber ?? p.phone ?? '',
            dateOfBirth: p.dateOfBirth ?? '',
            address: p.address ?? '',
            registeredOn: p.joinedOn ?? p.dateOfBirth ?? '',
            status: (p.status ?? 'active') as AccountStatus,
          })) as Patient[],
        ),
      error: () => {},
    });
  }
}
