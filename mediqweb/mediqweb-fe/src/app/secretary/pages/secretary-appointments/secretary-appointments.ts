import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  AppointmentStatusBadge,
  Avatar,
  Button,
  Card,
  Dropdown,
  FilterBar,
  Modal,
  Table,
  TableCell,
  type DropdownItem,
  type TableColumn,
} from '../../../shared/components';
import { formatDuration } from '../../secretary.dates';
import { SecretarySession } from '../../secretary-session';
import { ToastService } from '../../../core/services/toast.service';
import type { Appointment, AppointmentStatus } from '../../secretary.models';

type StatusFilter = 'all' | AppointmentStatus;

const STATUS_ITEMS: DropdownItem[] = [
  { id: 'all', label: 'All statuses' },
  { id: 'booked', label: 'Booked' },
  { id: 'confirmed', label: 'Confirmed' },
  { id: 'completed', label: 'Completed' },
  { id: 'cancelled', label: 'Cancelled' },
  { id: 'no-show', label: 'No-show' },
];

/** An appointment can still be called off while the patient is expected. */
const LIVE: readonly AppointmentStatus[] = ['booked', 'confirmed'];

/**
 * Appointment management: the Secretary's main screen.
 *
 * Two actions, and the page is built around that rather than around what it used
 * to offer. A visit arrives `booked` — the patient asked for it — and the desk
 * either confirms it or calls it off. Creating an appointment and moving a booked
 * one to a different time are not actions here at all, so there is no form, no
 * dialog and no state for either; `SecretarySession` does not expose the methods
 * that would perform them, so the absence is enforced below the view rather than
 * by it.
 *
 * Confirming is the Secretary's because it means "the patient said they are
 * coming", which is a phone call. Marking a visit complete and recording a no-show
 * stay the doctor's, so the two roles cover the lifecycle without both being able
 * to do the same thing.
 *
 * Scoped to the assigned doctor, so the page carries no doctor filter and no
 * doctor column: every row would say the same name.
 */
@Component({
  selector: 'app-secretary-appointments',
  imports: [
    DatePipe,
    RouterLink,
    AppointmentStatusBadge,
    Avatar,
    Button,
    Card,
    Dropdown,
    FilterBar,
    Modal,
    Table,
    TableCell,
  ],
  templateUrl: './secretary-appointments.html',
  styleUrl: './secretary-appointments.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecretaryAppointments {
  private readonly session = inject(SecretarySession);
  private readonly toasts = inject(ToastService);

  protected readonly query = signal('');
  protected readonly statusFilter = signal<StatusFilter>('all');
  protected readonly confirmCancelFor = signal<Appointment | null>(null);

  protected readonly statusOptions = STATUS_ITEMS;

  /** Whether any appointment can be confirmed or cancelled — used to hide the
   *  Actions column entirely when there is nothing actionable on screen. */
  protected readonly hasActions = computed(() =>
    this.appointments().some((a) => this.canConfirm(a) || this.canCancel(a)),
  );

  protected readonly columns: TableColumn<Appointment>[] = [
    { key: 'startsAt', header: 'When', sortable: true },
    { key: 'patientId', header: 'Patient' },
    { key: 'reason', header: 'Reason', hideBelow: 'lg' },
    { key: 'durationMinutes', header: 'Length', hideBelow: 'lg' },
    { key: 'status', header: 'Status', sortable: true },
  ];

  /** Columns with the Actions column appended only when something is actionable. */
  protected readonly columnsWithActions = computed(() =>
    this.hasActions()
      ? [
          ...this.columns,
          { key: 'actions', header: 'Actions', align: 'end' as const },
        ]
      : this.columns,
  );

  protected readonly appointments = computed(() => {
    const term = this.query().trim().toLowerCase();
    const status = this.statusFilter();

    return this.session.appointments().filter((appointment) => {
      if (status !== 'all' && appointment.status !== status) return false;
      if (!term) return true;
      // The patient's name as well as the reason: the doctor is the same on every
      // row, so searching for one would only ever match everything.
      return (
        this.session.patientName(appointment.patientId).toLowerCase().includes(term) ||
        appointment.reason.toLowerCase().includes(term)
      );
    });
  });

  protected readonly totalCount = computed(() => this.session.appointments().length);

  /** Still expected to happen: booked or confirmed, cancelled ones excluded. */
  protected readonly openCount = computed(
    () =>
      this.session.appointments().filter((a) => a.status === 'booked' || a.status === 'confirmed')
        .length,
  );

  /**
   * How many are waiting on this desk.
   *
   * Read from the store rather than counted here, so this page and the dashboard's
   * "Awaiting confirmation" tile cannot drift apart — and so it is desk-scoped the
   * same way the list is. The booking form used to open the page with a paragraph
   * about what a booking had to satisfy; with the form gone, this is the one fact
   * the page exists to act on, and it is the first thing on screen.
   */
  protected readonly awaitingConfirmationCount = computed(
    () => this.session.awaitingConfirmation().length,
  );

  protected onStatusFilterChange(item: DropdownItem): void {
    this.statusFilter.set(item.id as StatusFilter);
  }

  protected patientName(appointment: Appointment): string {
    return this.session.patientName(appointment.patientId);
  }

  protected length(appointment: Appointment): string {
    return formatDuration(appointment.durationMinutes);
  }

  /**
   * Whether Confirm is offered.
   *
   * `booked` only. An appointment that is already confirmed has nothing left to
   * confirm, so showing the button there would offer an action that does nothing
   * and report itself as successful.
   */
  protected canConfirm(appointment: Appointment): boolean {
    return appointment.status === 'booked';
  }

  /** Cancel is offered for as long as the patient is still expected. */
  protected canCancel(appointment: Appointment): boolean {
    return LIVE.includes(appointment.status);
  }

  /**
   * Confirms a booked appointment.
   *
   * Reports the actual outcome from the API: success, a harmless double-confirm,
   * a state/rule refusal, or a failed request (with rollback handled in session).
   */
  protected confirm(appointment: Appointment): void {
    const name = this.patientName(appointment);
    this.session.confirm(appointment.id).subscribe((outcome) => {
      switch (outcome.kind) {
        case 'confirmed':
          this.toasts.success('Appointment confirmed', `${name} is expected.`);
          return;
        case 'already-confirmed':
          this.toasts.info('Already confirmed', `${name} was confirmed a moment ago.`);
          return;
        case 'not-confirmable':
          this.toasts.error(
            'Not confirmed',
            `That appointment is now ${outcome.currentStatus ?? 'unavailable'}, so it cannot be confirmed.`,
          );
          return;
        case 'failed':
          this.toasts.error(
            'Not confirmed',
            outcome.message || 'The appointment could not be confirmed. Please try again.',
          );
          return;
      }
    });
  }

  protected requestCancel(appointment: Appointment): void {
    this.confirmCancelFor.set(appointment);
  }

  protected cancelCancel(): void {
    this.confirmCancelFor.set(null);
  }

  protected confirmCancel(): void {
    const appointment = this.confirmCancelFor();
    if (!appointment) return;
    this.confirmCancelFor.set(null);
    const name = this.patientName(appointment);

    this.session.cancel(appointment.id).subscribe((outcome) => {
      switch (outcome.kind) {
        case 'cancelled':
          this.toasts.warning(
            'Appointment cancelled',
            `${name} will not be seen. The visit stays in the history.`,
          );
          return;
        case 'not-cancellable':
          this.toasts.error('Not cancelled', 'That appointment could not be updated.');
          return;
        case 'failed':
          this.toasts.error(
            'Not cancelled',
            outcome.message || 'The appointment could not be cancelled. Please try again.',
          );
          return;
      }
    });
  }
}
