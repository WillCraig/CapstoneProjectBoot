import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateInteractionRequest,
  Interaction,
  UpdateInteractionRequest,
} from '../models/crm.models';

@Injectable({ providedIn: 'root' })
export class InteractionsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/customers';

  list(customerId: string): Observable<Interaction[]> {
    return this.http.get<Interaction[]>(
      `${this.baseUrl}/${encodeURIComponent(customerId)}/interactions`,
    );
  }

  create(customerId: string, request: CreateInteractionRequest): Observable<Interaction> {
    return this.http.post<Interaction>(
      `${this.baseUrl}/${encodeURIComponent(customerId)}/interactions`,
      request,
      { headers: { 'X-Actor': 'AGENT' } },
    );
  }

  update(
    customerId: string,
    interactionId: string,
    request: UpdateInteractionRequest,
  ): Observable<Interaction> {
    return this.http.put<Interaction>(
      `${this.baseUrl}/${encodeURIComponent(customerId)}/interactions/${encodeURIComponent(interactionId)}`,
      request,
    );
  }

  delete(customerId: string, interactionId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/${encodeURIComponent(customerId)}/interactions/${encodeURIComponent(interactionId)}`,
    );
  }
}
