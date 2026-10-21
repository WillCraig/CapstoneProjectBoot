import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { CustomerApiService } from './customer-api.service';
import { Customer } from './customer.model';

@Component({
  selector: 'app-customer-lookup-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './customer-lookup-page.component.html',
  styleUrl: './customer-pages.component.css',
})
export class CustomerLookupPageComponent {
  private readonly api = inject(CustomerApiService);

  readonly customers = signal<Customer[]>([]);
  readonly statusFilter = signal('ALL');
  readonly sortDirection = signal<'asc' | 'desc'>('asc');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  query = '';
  readonly visibleCustomers = computed(() => {
    const status = this.statusFilter();
    const direction = this.sortDirection() === 'asc' ? 1 : -1;
    return this.customers()
      .filter((customer) => status === 'ALL' || customer.status === status)
      .slice()
      .sort((a, b) => direction * a.customerId.localeCompare(b.customerId, undefined, { numeric: true }));
  });

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.search(this.query).subscribe({
      next: (customers) => {
        this.customers.set(customers);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not load customers'));
        this.loading.set(false);
      },
    });
  }

  updateStatusFilter(value: string): void {
    this.statusFilter.set(value);
  }

  updateSortDirection(value: string): void {
    this.sortDirection.set(value === 'desc' ? 'desc' : 'asc');
  }
}
