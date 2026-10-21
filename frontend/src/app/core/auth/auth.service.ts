import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenState = signal<string | null>(null);

  readonly isAuthenticated = () => this.tokenState() !== null;

  setToken(token: string | null): void {
    this.tokenState.set(token);
  }

  getToken(): string | null {
    return this.tokenState();
  }

  logout(): void {
    this.tokenState.set(null);
  }
}
