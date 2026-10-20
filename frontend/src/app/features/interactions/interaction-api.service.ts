import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateInteractionRequest, Interaction } from './interaction.model';

@Injectable({ providedIn: 'root' })
export class InteractionApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/interactions`;

  list(customerId: string): Observable<Interaction[]> {
    return this.http.get<Interaction[]>(this.base, {
      params: new HttpParams().set('customerId', customerId),
    });
  }

  create(body: CreateInteractionRequest): Observable<Interaction> {
    return this.http.post<Interaction>(this.base, body);
  }
}
