import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateCustomerRequest, Customer, UpdateCustomerRequest } from './customer.model';

/** Typed gateway to the customer API; components never build URLs themselves. */
@Injectable({ providedIn: 'root' })
export class CustomerApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/customers`;

  search(query?: string): Observable<Customer[]> {
    const term = query?.trim();
    const params = term ? new HttpParams().set('q', term) : new HttpParams();
    return this.http.get<Customer[]>(this.base, { params });
  }

  create(body: CreateCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.base, body);
  }

  getById(customerId: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.base}/${customerId}`);
  }

  update(customerId: string, body: UpdateCustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.base}/${customerId}`, body);
  }

  /** Closing a customer is the delete path; the row and its interactions are kept. */
  delete(customerId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${customerId}`);
  }
}
