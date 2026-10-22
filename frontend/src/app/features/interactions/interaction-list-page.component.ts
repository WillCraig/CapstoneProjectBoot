import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { LAB_CORRELATION_ID } from '../../core/correlation.interceptor';
import { CustomerApiService } from '../customers/customer-api.service';
import { Customer } from '../customers/customer.model';
import { InteractionApiService } from './interaction-api.service';
import { Interaction } from './interaction.model';

/**
 * Customer activity timeline. Jason's layout (customer picker fed by the customer API, quick
 * note box) combined with the loading state, Problem Details error messages, correlation id
 * display, and Amina/Ravi demo shortcuts from the Lab 50 page.
 */
@Component({
  selector: 'app-interaction-list-page',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './interaction-list-page.component.html',
  styleUrl: './interaction-list-page.component.css',
})
export class InteractionListPageComponent {
  private readonly api = inject(InteractionApiService);
  private readonly customerApi = inject(CustomerApiService);
  private readonly route = inject(ActivatedRoute);

  readonly correlationId = LAB_CORRELATION_ID;
  /** Customer whose timeline is shown; the picker input only takes effect on "Load activity". */
  readonly customerId = signal(
    this.route.snapshot.queryParamMap.get('customerId') ?? 'CUS-1001',
  );
  customerIdInput = this.customerId();
  summary = '';
  readonly customers = signal<Customer[]>([]);
  readonly rows = signal<Interaction[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly customerError = signal<string | null>(null);
  /** True when the note box itself was rejected, not when the timeline failed to load. */
  readonly summaryInvalid = signal(false);
  readonly savedNotice = signal('');

  constructor() {
    this.loadCustomers();
    this.reload();
  }

  /** Load the timeline for whatever is typed or picked in the customer box. */
  useCustomer(): void {
    this.savedNotice.set('');
    const customerId = this.customerIdInput.trim();
    this.customerIdInput = customerId;
    if (!customerId) {
      this.customerError.set('Enter a customer public ID.');
      this.rows.set([]);
      return;
    }
    this.customerError.set(null);
    this.customerId.set(customerId);
    this.reload();
  }

  /** Demo shortcut buttons (Amina CUS-1001, Ravi CUS-1002). */
  chooseCustomer(customerId: string): void {
    this.customerIdInput = customerId;
    this.useCustomer();
  }

  /** Quick note: records a NOTE for the selected customer, then refreshes the timeline. */
  create(): void {
    this.savedNotice.set('');
    const summary = this.summary.trim();
    if (!summary) {
      this.summaryInvalid.set(true);
      this.error.set('Write a note before recording it.');
      return;
    }
    this.summaryInvalid.set(false);
    this.error.set(null);
    this.saving.set(true);
    this.api
      .create({
        customerId: this.customerId(),
        interactionType: 'NOTE',
        summary,
        correlationId: LAB_CORRELATION_ID,
      })
      .subscribe({
        next: () => {
          this.summary = '';
          this.saving.set(false);
          this.savedNotice.set('Note recorded.');
          this.reload();
        },
        error: (err) => {
          this.saving.set(false);
          this.summaryInvalid.set(true);
          this.error.set(apiErrorMessage(err, 'Could not record this note'));
        },
      });
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(this.customerId()).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.rows.set([]);
        this.error.set(apiErrorMessage(err, 'Could not load interactions'));
        this.loading.set(false);
      },
    });
  }

  remove(row: Interaction): void {
    this.savedNotice.set('');
    this.error.set(null);
    this.api.delete(row.id).subscribe({
      next: () => this.reload(),
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not delete this interaction')),
    });
  }

  private loadCustomers(): void {
    this.customerApi.list().subscribe({
      next: (customers) => this.customers.set(customers),
      error: (err) =>
        this.customerError.set(apiErrorMessage(err, 'Could not load the customer list')),
    });
  }
}
