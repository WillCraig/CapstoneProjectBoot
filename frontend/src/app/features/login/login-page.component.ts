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
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css',
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
        void this.router.navigate(['/home']);
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
