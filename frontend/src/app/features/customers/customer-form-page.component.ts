import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { CustomerApiService } from './customer-api.service';
import { CustomerStatus } from './customer.model';

/**
 * One form page serves both routes: /customers/new creates, /customers/:id/edit updates.
 * The presence of the :id route parameter is the only difference, so the form fields and
 * validation can never drift apart between create and edit.
 */
@Component({
  selector: 'app-customer-form-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  styleUrl: './customer-pages.component.css',
  template: `
    <section class="page-card manage-page">
      <p class="eyebrow">Customer maintenance</p>
      <h2>{{ customerId ? 'Update customer' : 'Create new customer' }}</h2>

      @if (error()) {
        <p class="error" role="alert" data-testid="form-error">{{ error() }}</p>
      }

      @if (loading()) {
        <p class="empty-state" role="status">Loading customer…</p>
      } @else {
        <form class="customer-form" (ngSubmit)="save()">
          <label for="public-id">Public ID</label>
          <input
            id="public-id"
            name="publicId"
            [value]="customerId || 'Assigned automatically when created'"
            readonly
            aria-describedby="public-id-help"
          />
          <span id="public-id-help" class="field-help">
            This ID is assigned by the system and cannot be changed.
          </span>

          <label for="full-name">Full name</label>
          <input
            id="full-name"
            name="fullName"
            [(ngModel)]="fullName"
            required
            maxlength="200"
            autocomplete="name"
            placeholder="Enter customer's full name"
            data-testid="customer-name"
          />

          <label for="status">Customer status</label>
          <select id="status" name="status" [(ngModel)]="status" data-testid="customer-status">
            @for (option of statusOptions; track option) {
              <option [value]="option">{{ option }}</option>
            }
          </select>

          <div class="form-actions">
            <button type="submit" data-testid="save-customer" [disabled]="saving()">
              {{ saving() ? 'Saving…' : customerId ? 'Save changes' : 'Create customer' }}
            </button>
          </div>
        </form>
      }

      @if (!customerId) {
        <p class="update-hint">
          Need to change an existing customer? <a routerLink="/customers">Find a customer to update</a>.
        </p>
      }
      <div class="page-actions">
        <a class="back-button" routerLink="/customers">Back to customers</a>
      </div>
    </section>
  `,
})
export class CustomerFormPageComponent implements OnInit {
  private readonly api = inject(CustomerApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly session = inject(AuthSessionService);

  /** Null on /customers/new; the public ID on /customers/:id/edit. */
  customerId: string | null = null;
  fullName = '';
  status: CustomerStatus = 'PROSPECT';
  /** Status as loaded from the API, so an agent editing a CLOSED customer keeps CLOSED. */
  private loadedStatus: CustomerStatus | null = null;
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);

  /**
   * Closing and reopening are ADMIN-only (the API returns 403 otherwise), so agents never see
   * CLOSED as a choice, and an agent editing a CLOSED customer can only keep it CLOSED. UX only.
   */
  get statusOptions(): CustomerStatus[] {
    if (!this.customerId) {
      return ['PROSPECT', 'ACTIVE'];
    }
    if (this.session.isAdmin()) {
      return ['PROSPECT', 'ACTIVE', 'CLOSED'];
    }
    return this.loadedStatus === 'CLOSED' ? ['CLOSED'] : ['PROSPECT', 'ACTIVE'];
  }

  ngOnInit(): void {
    this.customerId = this.route.snapshot.paramMap.get('id');
    if (!this.customerId) {
      return;
    }
    this.loading.set(true);
    this.api.getById(this.customerId).subscribe({
      next: (customer) => {
        this.fullName = customer.fullName;
        this.status = customer.status;
        this.loadedStatus = customer.status;
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not load this customer'));
        this.loading.set(false);
      },
    });
  }

  save(): void {
    this.saving.set(true);
    this.error.set(null);
    const body = { fullName: this.fullName.trim(), status: this.status };
    const request = this.customerId
      ? this.api.update(this.customerId, body)
      : this.api.create({ fullName: body.fullName, status: this.creatableStatus() });
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        void this.router.navigate(['/customers', saved.customerId]);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not save this customer'));
        this.saving.set(false);
      },
    });
  }

  /** Create never offers CLOSED in the select; this keeps the type honest if state drifts. */
  private creatableStatus(): 'PROSPECT' | 'ACTIVE' {
    return this.status === 'ACTIVE' ? 'ACTIVE' : 'PROSPECT';
  }
}
