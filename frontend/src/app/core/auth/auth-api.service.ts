import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthSessionService, LoginResponse } from './auth-session.service';

export const LOGIN_PATH = '/api/v1/auth/login';

/** Exchanges demo credentials for a JWT and starts the in-memory session. */
@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(AuthSessionService);

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}${LOGIN_PATH}`, { username, password })
      .pipe(tap((response) => this.session.start(response)));
  }
}
