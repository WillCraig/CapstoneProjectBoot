import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
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
  template: `
    <section>
      <p><a routerLink="/customers">Back to customers</a></p>
      <h2>{{ customerId ? 'Edit customer' : 'New customer' }}</h2>

      @if (error()) {
        <p role="alert" data-testid="form-error">{{ error() }}</p>
      }

      @if (loading()) {
        <p>Loading…</p>
      } @else {
        <form (ngSubmit)="save()">
          <label>
            Full name
            <input
              name="fullName"
              [(ngModel)]="fullName"
              required
              maxlength="200"
              data-testid="customer-name"
            />
          </label>
          <label>
            Status
            <select name="status" [(ngModel)]="status" data-testid="customer-status">
              @for (option of statusOptions; track option) {
                <option [value]="option">{{ option }}</option>
              }
            </select>
          </label>
          <button type="submit" data-testid="save-customer" [disabled]="saving()">
            {{ saving() ? 'Saving…' : 'Save customer' }}
          </button>
        </form>
      }
    </section>
  `,
})
export class CustomerFormPageComponent implements OnInit {
  private readonly api = inject(CustomerApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** Null on /customers/new; the public ID on /customers/:id/edit. */
  customerId: string | null = null;
  fullName = '';
  status: CustomerStatus = 'PROSPECT';
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);

  get statusOptions(): CustomerStatus[] {
    return this.customerId ? ['PROSPECT', 'ACTIVE', 'CLOSED'] : ['PROSPECT', 'ACTIVE'];
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
