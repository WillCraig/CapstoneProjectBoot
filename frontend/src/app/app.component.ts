import { Component, DestroyRef, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthSessionService } from './core/auth/auth-session.service';

/** How often the header re-checks whether the 15-minute token has expired. */
export const SESSION_CHECK_MS = 30_000;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  readonly session = inject(AuthSessionService);
  private readonly router = inject(Router);

  constructor() {
    // The template only reads the token signal. The expiry check lives here, outside
    // rendering, because isAuthenticated() clears the session (a signal write) once the
    // token has expired, and Angular forbids signal writes while it renders a template.
    const timer = setInterval(() => this.checkExpiry(), SESSION_CHECK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  /** Signs the user out and returns to /login once the token has expired. */
  checkExpiry(now: number = Date.now()): void {
    if (this.session.accessToken() && !this.session.isAuthenticated(now)) {
      void this.router.navigate(['/login']);
    }
  }

  logout(): void {
    this.session.logout();
    void this.router.navigate(['/login']);
  }
}
