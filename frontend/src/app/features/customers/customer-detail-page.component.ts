import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { CelebrationService } from '../../core/celebration.service';
import { CustomerApiService } from './customer-api.service';
import { Customer } from './customer.model';

/**
 * Customer profile. This is the middle beat of the defense demo: search -> profile ->
 * interaction timeline. Editing and recording interactions hand off to their own pages.
 */
@Component({
  selector: 'app-customer-detail-page',
  standalone: true,
  imports: [DatePipe, RouterLink],
  styleUrl: './customer-pages.component.css',
  template: `
    <section class="page-card">
      <p class="eyebrow">Customer directory</p>
      <h2>Customer profile</h2>

      @if (error()) {
        <p class="error" role="alert" data-testid="detail-error">{{ error() }}</p>
      }

      @if (customer(); as current) {
        <dl class="profile-facts">
          <dt>Name</dt>
          <dd data-testid="profile-name">{{ current.fullName }}</dd>
          <dt>Customer ID</dt>
          <dd data-testid="profile-id">{{ current.customerId }}</dd>
          <dt>Status</dt>
          <dd data-testid="profile-status">{{ current.status }}</dd>
          <dt>Customer since</dt>
          <dd>{{ current.createdAt | date: 'medium' }}</dd>
        </dl>

        <p class="profile-actions">
          <a
            class="table-action"
            routerLink="/interactions"
            [queryParams]="{ customerId: current.customerId }"
            data-testid="view-interactions"
          >
            View interactions
          </a>
          <a
            class="table-action"
            routerLink="/interactions/new"
            [queryParams]="{ customerId: current.customerId }"
            data-testid="record-interaction"
          >
            Record an interaction
          </a>
          <a
            class="table-action"
            [routerLink]="['/customers', current.customerId, 'edit']"
            data-testid="edit-customer"
          >
            Edit customer
          </a>
        </p>

        @if (current.status !== 'CLOSED' && session.isAdmin()) {
          <button type="button" data-testid="close-customer" [disabled]="closing()" (click)="close()">
            {{ closing() ? 'Closing…' : 'Close customer' }}
          </button>
        } @else if (current.status !== 'CLOSED') {
          <p class="field-help" data-testid="close-requires-admin">Only an ADMIN can close a customer.</p>
        } @else {
          <p class="field-help" data-testid="closed-message">This customer is closed. History is kept for audit.</p>
        }
      } @else if (loading()) {
        <p class="empty-state" role="status">Loading customer…</p>
      }
      <div class="page-actions">
        <a class="back-button" routerLink="/customers">Back to customers</a>
      </div>
    </section>
  `,
})
export class CustomerDetailPageComponent implements OnInit {
  private readonly api = inject(CustomerApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly celebration = inject(CelebrationService);
  readonly session = inject(AuthSessionService);

  readonly customer = signal<Customer | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly closing = signal(false);

  ngOnInit(): void {
    const customerId = this.route.snapshot.paramMap.get('id');
    if (!customerId) {
      this.error.set('No customer ID was provided.');
      return;
    }
    this.load(customerId);
  }

  close(): void {
    const current = this.customer();
    if (!current) return;
    this.closing.set(true);
    this.error.set(null);
    this.api.delete(current.customerId).subscribe({
      next: () => {
        this.closing.set(false);
        // Only after the API succeeds; the confetti canvas survives the navigation.
        this.celebration.celebrate();
        void this.router.navigate(['/customers']);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not close this customer'));
        this.closing.set(false);
      },
    });
  }

  private load(customerId: string): void {
    this.loading.set(true);
    this.api.getById(customerId).subscribe({
      next: (customer) => {
        this.customer.set(customer);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not load this customer'));
        this.loading.set(false);
      },
    });
  }
}
