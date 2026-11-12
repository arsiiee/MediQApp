import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  Avatar,
  Card,
  Dropdown,
  FilterBar,
  StatusBadge,
  Table,
  TableCell,
  type BadgeTone,
  type DropdownItem,
  type TableColumn,
} from '../../../shared/components';
import type { AccountStatus } from '../../../shared/domain/account-status';
import { SecretarySession } from '../../secretary-session';

type StatusFilter = 'all' | AccountStatus;

const STATUS_ITEMS: DropdownItem[] = [
  { id: 'all', label: 'All accounts' },
  { id: 'active', label: 'Active' },
  { id: 'inactive', label: 'Inactive' },
];

/**
 * A display row, flattened from `PatientSummary`.
 *
 * `ui-table` sorts by comparing the raw value under a column key, so a column
 * holding an `Appointment` would compare `"[object Object]"` with itself and
 * silently never reorder — a sortable header that looks broken. Flattening to
 * primitives makes every sortable column real. `null` sorts to the end, which is
 * what the table's comparator already does.
 */
interface PatientRow {
  readonly id: string;
  readonly name: string;
  readonly phone: string;
  readonly email: string;
  /** ISO date, or `null` when not captured at registration. */
  readonly dateOfBirth: string | null;
  readonly status: AccountStatus;
  /** ISO timestamp, or `null` when the patient has never been seen. */
  readonly lastVisitAt: string | null;
  /** ISO timestamp, or `null` when nothing is booked. */
  readonly nextVisitAt: string | null;
  /** ID of the next live appointment, or `null` when nothing is booked. */
  readonly nextVisitId: string | null;
  /** Status of the next live appointment ('booked' | 'confirmed'), or `null`. */
  readonly nextVisitStatus: 'booked' | 'confirmed' | null;
  readonly visitCount: number;
}

/**
 * The assigned doctor's patients.
 *
 * Not derived from anybody's diary, unlike the Doctor area's equivalent list: a
 * Secretary opens a patient record in order to book them, so somebody with no
 * appointments yet still has to appear here — otherwise a walk-in could never be
 * registered, which is the one case the desk most needs to handle. That is what
 * the patient's panel is for: membership comes from the assignment, not from a
 * visit somebody has not had yet.
 */
@Component({
  selector: 'app-secretary-patients',
  imports: [DatePipe, RouterLink, Avatar, Card, Dropdown, FilterBar, StatusBadge, Table, TableCell],
  templateUrl: './secretary-patients.html',
  styleUrl: './secretary-patients.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecretaryPatients {
  private readonly session = inject(SecretarySession);

  protected readonly query = signal('');
  protected readonly statusFilter = signal<StatusFilter>('all');

  protected readonly statusOptions = STATUS_ITEMS;

  protected readonly columns: TableColumn<PatientRow>[] = [
    { key: 'name', header: 'Patient', sortable: true },
    { key: 'dateOfBirth', header: 'Date of birth', sortable: true },
    { key: 'phone', header: 'Contact', hideBelow: 'md' },
    { key: 'lastVisitAt', header: 'Last visit', sortable: true },
    { key: 'nextVisitAt', header: 'Next appointment', sortable: true },
    { key: 'visitCount', header: 'Visits', sortable: true, align: 'end' },
    { key: 'status', header: 'Account', sortable: true },
    { key: 'actions', header: 'Actions', align: 'end' },
  ];

  protected readonly rows = computed<PatientRow[]>(() => {
    const term = this.query().trim().toLowerCase();
    const status = this.statusFilter();

    return this.session
      .patients()
      .filter((summary) => {
        if (status !== 'all' && summary.patient.status !== status) return false;
        if (!term) return true;
        // Search the contact details too: a secretary reading a name off a card
        // often has the number instead.
        return (
          summary.patient.name.toLowerCase().includes(term) ||
          summary.patient.email.toLowerCase().includes(term) ||
          summary.patient.phone.toLowerCase().includes(term)
        );
      })
      .map((summary): PatientRow => ({
        id: summary.patient.id,
        name: summary.patient.name,
        dateOfBirth: summary.patient.dateOfBirth,
        phone: summary.patient.phone,
        email: summary.patient.email,
        status: summary.patient.status,
        lastVisitAt: summary.lastVisit?.startsAt ?? null,
        nextVisitAt: summary.nextVisit?.startsAt ?? null,
        nextVisitId: summary.nextVisit?.id ?? null,
        nextVisitStatus: (summary.nextVisit?.status as 'booked' | 'confirmed' | null) ?? null,
        visitCount: summary.visitCount,
      }));
  });

  /** How many of the patients on this desk have something booked. */
  protected readonly bookedCount = computed(
    () => this.session.patients().filter((summary) => summary.nextVisit !== null).length,
  );

  protected readonly totalCount = computed(() => this.session.patients().length);

  /** Null while unassigned, so the empty state can say why it is empty. */
  protected readonly assignedDoctor = computed(() => this.session.assignedDoctor());

  protected readonly unassignedMessage = this.session.unassignedMessage;

  protected onStatusFilterChange(item: DropdownItem): void {
    this.statusFilter.set(item.id as StatusFilter);
  }

  protected statusTone(status: AccountStatus): BadgeTone {
    return status === 'active' ? 'success' : 'neutral';
  }

  /** Cancels an appointment on this desk, with an optional reason shown to the patient. */
  protected confirm(appointmentId: string): void {
    this.session.confirm(appointmentId);
  }

  /** Prompts for a cancellation reason, then cancels the appointment. */
  protected cancelWithReason(appointmentId: string): void {
    const reason = prompt('Reason for cancelling (shown to the patient):');
    if (reason !== null) {
      this.session.cancel(appointmentId, reason.trim() || null);
    }
  }
}
