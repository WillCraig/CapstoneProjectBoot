import { Component, DestroyRef, inject, NgZone } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthSessionService } from './core/auth/auth-session.service';

/** How often the header re-checks whether the 15-minute token has expired. */
export const SESSION_CHECK_MS = 30_000;

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
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
    const destroyRef = inject(DestroyRef);
    const zone = inject(NgZone);
    const timer = setInterval(() => this.checkExpiry(), SESSION_CHECK_MS);
    let focusTimer: ReturnType<typeof setTimeout> | undefined;
    destroyRef.onDestroy(() => {
      clearInterval(timer);
      clearTimeout(focusTimer);
    });

    // After a route change, move focus to the new page's h1 so keyboard and screen-reader
    // users land on the content instead of staying on the link they just activated.
    // The timer stays outside the Angular zone so unit tests are not left waiting on it.
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed(),
    ).subscribe(() => {
      clearTimeout(focusTimer);
      zone.runOutsideAngular(() => {
        focusTimer = setTimeout(() => document.querySelector<HTMLElement>('main h1')?.focus());
      });
    });
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
