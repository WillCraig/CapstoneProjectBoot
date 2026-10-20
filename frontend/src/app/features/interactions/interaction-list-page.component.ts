import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CustomerApiService } from '../customers/customer-api.service';
import { Customer } from '../customers/customer.model';
import { InteractionApiService } from './interaction-api.service';
import { Interaction } from './interaction.model';

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

  readonly customerId = signal('CUS-1001');
  customerIdInput = this.customerId();
  summary = 'Follow-up on billing question';
  readonly customers = signal<Customer[]>([]);
  readonly rows = signal<Interaction[]>([]);
  readonly error = signal<string | null>(null);
  readonly customerError = signal<string | null>(null);

  constructor() {
    this.loadCustomers();
    this.reload();
  }

  useCustomer(): void {
    const customerId = this.customerIdInput.trim();
    if (!customerId) {
      this.customerError.set('Enter a customer public ID.');
      return;
    }

    this.customerError.set(null);
    this.error.set(null);
    this.customerId.set(customerId);
    this.customerIdInput = customerId;
    this.reload();
  }

  create(): void {
    this.error.set(null);
    this.api
      .create({
        customerId: this.customerId(),
        interactionType: 'NOTE',
        summary: this.summary,
        correlationId: 'lab-request-001',
      })
      .subscribe({
        next: () => this.reload(),
        error: (err) => this.error.set(`Create failed (${err.status ?? 'network'})`),
      });
  }

  private loadCustomers(): void {
    this.customerApi.list().subscribe({
      next: (customers) => this.customers.set(customers),
      error: (err) => this.customerError.set(`Could not load customers (${err.status ?? 'network'})`),
    });
  }

  private reload(): void {
    this.api.list(this.customerId()).subscribe({
      next: (rows) => this.rows.set(rows),
      error: (err) => this.error.set(`List failed (${err.status ?? 'network'})`),
    });
  }
}
