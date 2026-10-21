import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthSessionService } from './auth-session.service';

/** UX only: sends signed-out users to /login. The API enforces access on every call. */
export const authGuard: CanActivateFn = () => {
  const session = inject(AuthSessionService);
  const router = inject(Router);
  return session.isAuthenticated() ? true : router.parseUrl('/login');
};
