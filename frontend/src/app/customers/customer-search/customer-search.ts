import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CustomersService } from '../../core/api/customers.service';
import { Customer } from '../../core/models/crm.models';
@Component({
  selector: 'app-customer-search',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './customer-search.html',
  styleUrl: './customer-search.css',
})
export class CustomerSearch {
  private readonly customersService = inject(CustomersService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  customerId = 'CUS-1001';
  customer: Customer | null = null;
  loading = false;
  errorMessage = '';

  search(): void {
    console.log('SEARCH METHOD CALLED');
    const id = this.customerId.trim();

    if (!id) {
      this.errorMessage = 'Enter a customer ID.';
      this.customer = null;
      this.loading = false;
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.customer = null;

    this.customersService.getById(id).subscribe({
      next: (customer) => {
        this.customer = customer;
        this.loading = false;
        this.errorMessage = '';
        this.cdr.detectChanges();

        console.log('Customer assigned:', this.customer);
        console.log('Loading state:', this.loading);
      },
      error: (error) => {
        this.loading = false;
        console.error('Customer request failed:', error);

        this.errorMessage =
          error.status === 404
            ? `Customer ${id} was not found.`
            : error.status === 401
              ? 'Your session is not authorized. Please sign in.'
              : 'Could not load the customer. Check that the API is running and the endpoint is available.';
      },
    });
  }

  openProfile(): void {
    if (this.customer) {
      this.router.navigate(['/customers', this.customer.customerId]);
    }
  }
}
