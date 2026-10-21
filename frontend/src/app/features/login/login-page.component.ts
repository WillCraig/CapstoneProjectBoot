import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { AuthApiService } from '../../core/auth/auth-api.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section>
      <h2>Sign in</h2>
      <p>
        Demo users: <code>agent</code> (AGENT) and <code>admin</code> (AGENT + ADMIN). Passwords
        come from the backend configuration. The token is kept in memory only.
      </p>
      <p>XSS probe (must stay text): {{ xssProbe }}</p>

      @if (error()) {
        <p role="alert" data-testid="login-error">{{ error() }}</p>
      }

      <form (ngSubmit)="login()">
        <label>
          Username
          <input
            name="username"
            [(ngModel)]="username"
            required
            autocomplete="username"
            data-testid="login-username"
          />
        </label>
        <label>
          Password
          <input
            name="password"
            type="password"
            [(ngModel)]="password"
            required
            autocomplete="current-password"
            data-testid="login-password"
          />
        </label>
        <button type="submit" data-testid="login-submit" [disabled]="busy()">
          {{ busy() ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>
    </section>
  `,
})
export class LoginPageComponent {
  private readonly auth = inject(AuthApiService);
  private readonly router = inject(Router);

  readonly xssProbe = '<img src=x onerror=alert(1)> CUS-1001';

  username = '';
  password = '';
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  login(): void {
    this.busy.set(true);
    this.error.set(null);
    this.auth.login(this.username.trim(), this.password).subscribe({
      next: () => {
        this.password = '';
        this.busy.set(false);
        void this.router.navigate(['/customers']);
      },
      error: (err: unknown) => {
        this.password = '';
        this.busy.set(false);
        this.error.set(
          err instanceof HttpErrorResponse && err.status === 401
            ? 'Invalid username or password.'
            : apiErrorMessage(err, 'Could not sign in'),
        );
      },
    });
  }
}
