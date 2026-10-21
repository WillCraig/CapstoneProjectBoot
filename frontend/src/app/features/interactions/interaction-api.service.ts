import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateInteractionRequest,
  Interaction,
  UpdateInteractionRequest,
} from './interaction.model';

/** Typed gateway to the Lab 49 interaction API; components never build URLs themselves. */
@Injectable({ providedIn: 'root' })
export class InteractionApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/interactions`;

  list(customerId: string): Observable<Interaction[]> {
    const params = new HttpParams().set('customerId', customerId);
    return this.http.get<Interaction[]>(this.base, { params });
  }

  create(body: CreateInteractionRequest): Observable<Interaction> {
    return this.http.post<Interaction>(this.base, body);
  }

  getById(id: string): Observable<Interaction> {
    return this.http.get<Interaction>(`${this.base}/${encodeURIComponent(id)}`);
  }

  update(id: string, body: UpdateInteractionRequest): Observable<Interaction> {
    return this.http.put<Interaction>(`${this.base}/${encodeURIComponent(id)}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${encodeURIComponent(id)}`);
  }
}
