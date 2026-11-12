import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Avatar, Card, StatusBadge, type BadgeTone } from '../../../shared/components';
import { DAY_NAMES, formatDuration } from '../../secretary.dates';
import { SecretarySession } from '../../secretary-session';
import type { DoctorSummary, ScheduleDay } from '../../secretary.models';

/**
 * The assigned doctor's published week.
 */
@Component({
  selector: 'app-secretary-schedules',
  imports: [Avatar, Card, StatusBadge],
  templateUrl: './secretary-schedules.html',
  styleUrl: './secretary-schedules.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecretarySchedules {
  private readonly session = inject(SecretarySession);

  protected readonly dayNames = DAY_NAMES;

  /** The one doctor on this desk, so an empty list means unassigned. */
  protected readonly doctors = computed<DoctorSummary[]>(() => this.session.doctors());

  protected readonly unassignedMessage = this.session.unassignedMessage;

  /** A doctor's week, sorted Sunday-first, ready for the grid. */
  protected daysFor(summary: DoctorSummary): ScheduleDay[] {
    return this.session.scheduleFor(summary.doctor.id);
  }

  protected isEnabled(day: ScheduleDay): boolean {
    return day.sessions.length > 0;
  }

  /** Renders all published windows for the day, or an em dash when closed. */
  protected windows(day: ScheduleDay): readonly string[] {
    if (!day.sessions.length) return ['—'];
    return day.sessions.map((session) => `${session.startTime} – ${session.endTime}`);
  }

  /** The summary line above the week's hours. */
  protected publishedFor(summary: DoctorSummary): string {
    const days = this.daysFor(summary);
    const open = days.filter((day) => day.sessions.length > 0).length;
    const minutes = this.session.weeklyMinutes(summary.doctor.id);
    if (!open) return 'Not publishing any hours';

    return `${open} ${open === 1 ? 'day' : 'days'} · ${formatDuration(minutes)} a week`;
  }

  protected statusTone(summary: DoctorSummary): BadgeTone {
    return summary.doctor.status === 'active' ? 'success' : 'neutral';
  }

  /** Appointments this doctor has left to come, for the "how busy" footnote. */
  protected bookedFor(summary: DoctorSummary): number {
    const now = this.session.now().getTime();
    return this.session
      .appointmentsForDoctor(summary.doctor.id)
      .filter(
        (appointment) =>
          (appointment.status === 'booked' || appointment.status === 'confirmed') &&
          new Date(appointment.startsAt).getTime() >= now,
      ).length;
  }
}
