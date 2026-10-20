import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Customer, SaveCustomerRequest } from './customer.model';

@Injectable({ providedIn: 'root' })
export class CustomerApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/customers`;

  list(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.base);
  }

  get(customerId: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.base}/${encodeURIComponent(customerId)}`);
  }

  create(body: SaveCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.base, body);
  }

  update(customerId: string, body: SaveCustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.base}/${encodeURIComponent(customerId)}`, body);
  }
}
