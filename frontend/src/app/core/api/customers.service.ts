import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer } from '../models/crm.models';

@Injectable({ providedIn: 'root' })
export class CustomersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/customers';

  list(page = 0, size = 20): Observable<unknown> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<unknown>(this.baseUrl, { params });
  }

  getById(customerId: string): Observable<Customer> {
    return this.http.get<Customer>(
      `${this.baseUrl}/${encodeURIComponent(customerId)}`
    );
  }
}
