import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { CustomerApiService } from './customer-api.service';
import { Customer } from './customer.model';

/**
 * Agent search is the capstone landing journey: find a customer by name or public ID,
 * open the profile, then work the interaction timeline from there.
 */
@Component({
  selector: 'app-customer-list-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  template: `
    <section>
      <h2>Customers</h2>
      <p><a routerLink="/customers/new" data-testid="new-customer">New customer</a></p>

      <form (ngSubmit)="reload()">
        <label>
          Search by name or customer ID
          <input name="query" [(ngModel)]="query" data-testid="customer-search" />
        </label>
        <button type="submit" data-testid="search-customers" [disabled]="loading()">
          {{ loading() ? 'Searching…' : 'Search' }}
        </button>
      </form>

      @if (error()) {
        <p role="alert" data-testid="list-error">{{ error() }}</p>
      } @else if (!loading() && rows().length === 0) {
        <p data-testid="empty-state">No customers match that search.</p>
      }

      <ul>
        @for (row of rows(); track row.customerId) {
          <li data-testid="customer-row">
            <a [routerLink]="['/customers', row.customerId]" data-testid="customer-profile">
              {{ row.fullName }}
            </a>
            ({{ row.customerId }}) — {{ row.status }}
          </li>
        }
      </ul>
    </section>
  `,
})
export class CustomerListPageComponent implements OnInit {
  private readonly api = inject(CustomerApiService);

  query = '';
  readonly rows = signal<Customer[]>([]);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.search(this.query).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.rows.set([]);
        this.error.set(apiErrorMessage(err, 'Could not load customers'));
        this.loading.set(false);
      },
    });
  }
}
