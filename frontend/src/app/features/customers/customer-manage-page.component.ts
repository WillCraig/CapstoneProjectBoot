import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { CustomerApiService } from './customer-api.service';
import { CustomerStatus } from './customer.model';

@Component({
  selector: 'app-customer-manage-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './customer-manage-page.component.html',
  styleUrl: './customer-pages.component.css',
})
export class CustomerManagePageComponent {
  private readonly api = inject(CustomerApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly session = inject(AuthSessionService);

  readonly isEdit = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  customerId = '';
  fullName = '';
  status: CustomerStatus = 'PROSPECT';
  private loadedStatus: CustomerStatus | null = null;

  get statuses(): CustomerStatus[] {
    if (!this.isEdit()) {
      return ['PROSPECT', 'ACTIVE'];
    }
    if (this.session.isAdmin()) {
      return ['PROSPECT', 'ACTIVE', 'CLOSED'];
    }
    return this.loadedStatus === 'CLOSED' ? ['CLOSED'] : ['PROSPECT', 'ACTIVE'];
  }

  constructor() {
    this.route.paramMap.subscribe((params) => {
      const customerId = params.get('customerId');
      this.error.set(null);
      if (!customerId) {
        this.isEdit.set(false);
        this.customerId = '';
        this.fullName = '';
        this.status = 'PROSPECT';
        this.loading.set(false);
        return;
      }

      this.isEdit.set(true);
      this.customerId = customerId;
      this.loading.set(true);
      this.api.get(customerId).subscribe({
        next: (customer) => {
          this.customerId = customer.customerId;
          this.fullName = customer.fullName;
          this.status = customer.status;
          this.loadedStatus = customer.status;
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(`Could not load customer (${err.status ?? 'network'})`);
          this.loading.set(false);
        },
      });
    });
  }

  save(): void {
    this.error.set(null);
    this.success.set(null);
    this.saving.set(true);
    const fullName = this.fullName.trim();
    const operation = this.isEdit()
      ? this.api.update(this.customerId, { fullName, status: this.status })
      : this.api.create({
          fullName,
          status: this.status === 'ACTIVE' ? 'ACTIVE' : 'PROSPECT',
        });

    operation.subscribe({
      next: (customer) => {
        this.customerId = customer.customerId;
        this.fullName = customer.fullName;
        this.status = customer.status;
        this.saving.set(false);
        this.success.set(this.isEdit() ? 'Customer updated.' : 'Customer created.');
        if (!this.isEdit()) {
          void this.router.navigate(['/customers/manage', customer.customerId]);
        }
      },
      error: (err) => {
        this.error.set(`Could not save customer (${err.status ?? 'network'})`);
        this.saving.set(false);
      },
    });
  }

  returnToChoices(): void {
    void this.router.navigate(['/home']);
  }
}
