import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterRenderEffect,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Avatar, Button, Card, StatusBadge } from '../../../shared/components';
import { ToastService } from '../../../core/services/toast.service';
import { dayLabel, relativeStamp } from '../../secretary.dates';
import { SecretarySession } from '../../secretary-session';
import type { Conversation, ConversationMessage } from '../../secretary.models';

/** A day separator in the thread, so a conversation spanning days stays readable. */
interface DayDivider {
  readonly kind: 'day';
  readonly key: string;
  readonly label: string;
}

/** One message bubble. */
interface MessageRow {
  readonly kind: 'message';
  readonly key: string;
  readonly message: ConversationMessage;
}

type ThreadRow = DayDivider | MessageRow;

/**
 * The Secretary's message desk.
 *
 * Two panes over one store: a list of threads on the left, the open thread on the
 * right. Deliberately one screen rather than a list screen plus a detail route â€”
 * the work here is comparing a thread against the others ("has anybody else
 * asked about Saturday?"), which a route change would hide.
 *
 * Scoped to the assigned doctor, like every other screen here: a thread with one
 * of this doctor's patients, or with the doctor themselves. The bodies name people
 * and quote their appointments, so a thread that survived a name-based filter
 * while carrying another patient's details would be the worst kind of leak â€” the
 * filter is on the party, in the store.
 *
 * Sending a reply appends to the in-memory session, exactly as booking appends to
 * the appointments, so the round trip is demonstrable end to end with no backend.
 * That is the whole feature: there is no transport, no delivery state and no
 * other party actually replying.
 */
@Component({
  selector: 'app-secretary-messages',
  imports: [DatePipe, Avatar, Button, Card, StatusBadge],
  templateUrl: './secretary-messages.html',
  styleUrl: './secretary-messages.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecretaryMessages {
  private readonly session = inject(SecretarySession);
  private readonly toasts = inject(ToastService);
  private readonly route = inject(ActivatedRoute);

  /**
   * The open thread, defaulting to the most recently active one.
   *
   * Read once at construction rather than tracked, so sending a reply â€” which
   * re-sorts the list â€” cannot quietly move the reader to a different thread
   * while they are part-way through typing into it.
   *
   * The default is deliberately *not* treated as opening the thread: it does not
   * mark anything read. Loading a page is not the same as working through a
   * queue, and a badge that empties itself on arrival tells the user nothing.
   */
  private readonly selectedIdState = signal<string | null>(null);

  protected readonly selectedId = computed<string | null>(() => {
    const manual = this.selectedIdState();
    if (manual) return manual;
    return this.session.conversations()[0]?.conversation.id ?? null;
  });

  /** What is in the composer. A signal rather than a form control so `canSend` can be derived. */
  protected readonly draft = signal('');

  protected readonly conversations = this.session.conversations;
  protected readonly selected = computed(() => {
    const id = this.selectedId();
    return id ? this.session.conversationById(id) : null;
  });

  protected readonly canSend = computed(() => this.draft().trim().length > 0);

  /**
   * The open thread flattened for `@for`, with a divider inserted whenever the
   * calendar day changes.
   *
   * Interleaving the dividers here rather than tracking "last day" inside the
   * template keeps the comparison next to the iteration it belongs to, and means
   * the template never has to know why a divider appeared.
   */
  protected readonly threadRows = computed<ThreadRow[]>(() => {
    const summary = this.selected();
    if (!summary) return [];

    const now = this.session.now();
    const rows: ThreadRow[] = [];
    let day = '';

    for (const message of summary.messages) {
      const key = message.sentAt.slice(0, 10);
      if (key !== day) {
        day = key;
        rows.push({ kind: 'day', key: `day-${key}`, label: dayLabel(message.sentAt, now) });
      }
      rows.push({ kind: 'message', key: message.id, message });
    }

    return rows;
  });

  /** Where new messages appear without the reader having to scroll for them. */
  private readonly thread = viewChild<ElementRef<HTMLElement>>('thread');

  constructor() {
    // A `?reply=` query param pre-fills the composer, so a link or button that
    // lands a Secretary on this page with a draft already waiting can save them
    // retyping. The draft is preserved when a thread is opened, so a pre-filled
    // reply survives thread selection and is always sent to the currently open
    // conversation.
    const reply = this.route.snapshot.queryParamMap.get('reply');
    if (reply) {
      this.draft.set(reply);
    }

    // Scroll the open thread to the newest message, on opening a thread and on
    // sending a reply.
    //
    // `afterRenderEffect` rather than `effect`, and the reason is specific: with
    // the thread now a scrolling panel, an ordinary effect measures the element
    // before the browser has laid the new message out, so it scrolled to the
    // bottom of the *previous* content â€” landing exactly one bubble short. That
    // went unnoticed while the thread was short and `scrollTop` of 0 happened to
    // be the bottom anyway. Measuring in `earlyRead` and assigning in `write` is
    // what puts the two in the right order relative to layout.
    afterRenderEffect({
      earlyRead: () => {
        // Read `threadRows` to depend on the open thread and its contents, so this
        // re-runs when either changes and not on every unrelated render.
        this.threadRows();
        const element = this.thread()?.nativeElement;
        return element ? { element, bottom: element.scrollHeight } : null;
      },
      write: (measured) => {
        const state = measured();
        if (state) state.element.scrollTop = state.bottom;
      },
    });
  }

  /**
   * Opens a thread and clears anything unread in it.
   *
   * The draft is intentionally preserved: it may have been pre-filled from a
   * `?reply=` query param or typed ahead, and the composer already shows which
   * thread it is addressed to via the header and label.
   */
  protected onSelect(conversationId: string): void {
    this.selectedIdState.set(conversationId);
    this.session.markConversationRead(conversationId);
    this.session.loadConversationMessages(conversationId);
  }

  protected onDraft(event: Event): void {
    this.draft.set((event.target as HTMLTextAreaElement).value);
  }

  /**
   * Enter sends, Shift + Enter starts a new line.
   *
   * Handled on the textarea rather than with a global key listener so it only
   * applies while the composer has focus â€” a stray Enter elsewhere on the page
   * must not send a patient's reply.
   */
  protected onComposerKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter' || event.shiftKey) return;
    event.preventDefault();
    this.send();
  }

  /**
   * Appends the draft to the open thread and confirms it landed.
   *
   * The draft is only cleared when the store accepted it, so a refused send does
   * not throw away what somebody typed. The confirmation is gated on that same
   * answer for the same reason: an empty body or a thread that is not there is a
   * refusal, and saying "Message sent" over one would be the one claim in this
   * screen that could be checked and found untrue.
   *
   * `info` rather than `success`, because the bubble appearing directly under the
   * cursor is the confirmation and a toast repeating it is noise. It is the one
   * place on this page a notification is worth the interruption.
   */
  protected send(): void {
    const summary = this.selected();
    if (!summary) {
      this.toasts.warning(
        'No conversation selected',
        'Select a patient chat before sending a reply.',
      );
      return;
    }

    this.session.sendMessage(summary.conversation.id, this.draft()).subscribe({
      next: (sent) => {
        if (!sent) {
          this.toasts.warning('Nothing sent', 'Write a message before pressing send.');
          return;
        }
        this.draft.set('');
        this.toasts.info(
          'Message sent',
          `Added to your reply to ${summary.name}. Nothing is delivered yet — see the note below.`,
        );
      },
      error: () => {
        this.toasts.error('Send failed', 'Your message could not be delivered. Please try again.');
      },
    });
  }

  /** Relative stamp for a list row: "12 min ago", "Yesterday", "4 Mar". */
  protected stamp(iso: string): string {
    return relativeStamp(iso, this.session.now());
  }

  /** The line under a name: "Patient Â· 2 visits", "Doctor Â· Cardiology". */
  protected subtitle(conversation: Conversation): string {
    return this.session.conversationSubtitle(conversation);
  }
}
