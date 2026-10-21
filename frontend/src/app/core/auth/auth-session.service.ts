import { Injectable, computed, signal } from '@angular/core';

export type Role = 'AGENT' | 'ADMIN';

/** Body of `POST /api/v1/auth/login` (see backend AuthController). */
export interface LoginResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  username: string;
  roles: Role[];
}

/**
 * Holds the signed-in user's JWT **in memory only** (an Angular signal), never in
 * localStorage/sessionStorage. Trade-off: a page refresh signs the user out, but an injected
 * script cannot lift a token out of web storage, and the backend token lives 15 minutes.
 *
 * Roles here only shape the UI (e.g. hide "Close customer" for agents). The Spring Boot API
 * re-checks every request, so editing these signals in devtools grants nothing.
 */
@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  readonly accessToken = signal<string | null>(null);
  readonly username = signal<string | null>(null);
  readonly roles = signal<Role[]>([]);
  private readonly expiresAt = signal<number>(0);

  readonly isAdmin = computed(() => this.roles().includes('ADMIN'));

  start(response: LoginResponse, now: number = Date.now()): void {
    this.accessToken.set(response.accessToken);
    this.username.set(response.username);
    this.roles.set(response.roles);
    this.expiresAt.set(now + response.expiresIn * 1000);
  }

  /** True while a token is held and not past its expiry. */
  isAuthenticated(now: number = Date.now()): boolean {
    if (!this.accessToken()) {
      return false;
    }
    if (now >= this.expiresAt()) {
      this.logout();
      return false;
    }
    return true;
  }

  logout(): void {
    this.accessToken.set(null);
    this.username.set(null);
    this.roles.set([]);
    this.expiresAt.set(0);
  }
}
