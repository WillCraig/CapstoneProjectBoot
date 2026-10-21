import { DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CustomersService } from '../../core/api/customers.service';
import { Customer } from '../../core/models/crm.models';

@Component({
  selector: 'app-customer-profile',
  standalone: true,
  imports: [RouterLink, DatePipe],
  templateUrl: './customer-profile.html',
  styleUrl: './customer-profile.css',
})
export class CustomerProfile implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly customersService = inject(CustomersService);
  private readonly cdr = inject(ChangeDetectorRef);

  customer: Customer | null = null;
  loading = true;
  errorMessage = '';

  ngOnInit(): void {
    const customerId = this.route.snapshot.paramMap.get('id');

    if (!customerId) {
      this.errorMessage = 'No customer ID was provided.';
      this.loading = false;
      return;
    }

    this.customersService
      .getById(customerId)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (customer) => {
          this.customer = customer;
          this.cdr.detectChanges();
        },
        error: (error) => {
          this.errorMessage =
            error.status === 404
              ? `Customer ${customerId} was not found.`
              : 'Unable to load the customer profile. Check API availability and authorization.';
          this.cdr.detectChanges();
        },
      });
  }
}
