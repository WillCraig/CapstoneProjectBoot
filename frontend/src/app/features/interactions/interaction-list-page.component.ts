import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { LAB_CORRELATION_ID } from '../../core/correlation.interceptor';
import { InteractionApiService } from './interaction-api.service';
import { Interaction } from './interaction.model';

@Component({
  selector: 'app-interaction-list-page',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <section class="page">
      <div class="page-heading">
        <div>
          <h2>Customer interactions</h2>
          <p class="muted">Correlation <code>{{ correlationId }}</code></p>
        </div>
        <a
          class="button-link"
          data-testid="new-interaction"
          [routerLink]="['/interactions/new']"
          [queryParams]="{ customerId: customerId }"
        >
          New interaction
        </a>
      </div>

      <form class="search-form" (ngSubmit)="reload()">
        <label>
          Customer ID
          <input name="customerId" [(ngModel)]="customerId" required data-testid="customer-id" />
        </label>
        <button type="submit" data-testid="load-interactions" [disabled]="loading()">
          {{ loading() ? 'Loading…' : 'Load interactions' }}
        </button>
      </form>

      <div class="fixture-actions" aria-label="Demo customers">
        <span>Demo customers:</span>
        <button type="button" data-testid="customer-amina" (click)="chooseCustomer('CUS-1001')">
          Amina Khan (CUS-1001)
        </button>
        <button type="button" data-testid="customer-ravi" (click)="chooseCustomer('CUS-1002')">
          Ravi Singh (CUS-1002)
        </button>
      </div>

      @if (error()) {
        <p class="notice notice-error" role="alert" data-testid="list-error">{{ error() }}</p>
      } @else if (!loading() && rows().length === 0) {
        <p class="notice" data-testid="empty-state">No interactions recorded for this customer yet.</p>
      }

      <ul class="interaction-list">
        @for (row of rows(); track row.id) {
          <li data-testid="interaction-row">
            <div class="interaction-main">
              <strong>{{ row.interactionType }}</strong>
              <span>{{ row.summary }}</span>
              <small>
                {{ row.createdAt | date: 'medium' }} · {{ row.correlationId }}
              </small>
            </div>
            <div class="row-actions">
              <a [routerLink]="['/interactions', row.id]" data-testid="interaction-detail">Details</a>
              <button
                type="button"
                class="danger"
                data-testid="delete-interaction"
                (click)="remove(row)"
              >
                Delete
              </button>
            </div>
          </li>
        }
      </ul>
    </section>
  `,
})
export class InteractionListPageComponent implements OnInit {
  private readonly api = inject(InteractionApiService);
  private readonly route = inject(ActivatedRoute);

  readonly correlationId = LAB_CORRELATION_ID;
  customerId = 'CUS-1001';
  readonly rows = signal<Interaction[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  ngOnInit(): void {
    this.customerId = this.route.snapshot.queryParamMap.get('customerId') ?? this.customerId;
    this.reload();
  }

  chooseCustomer(customerId: string): void {
    this.customerId = customerId;
    this.reload();
  }

  reload(): void {
    const customerId = this.customerId.trim();
    this.customerId = customerId;
    if (!customerId) {
      this.error.set('Enter a customer ID.');
      this.rows.set([]);
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.api.list(customerId).subscribe({
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
    this.error.set(null);
    this.api.delete(row.id).subscribe({
      next: () => this.reload(),
      error: (err) => this.error.set(apiErrorMessage(err, 'Could not delete this interaction')),
    });
  }
}
