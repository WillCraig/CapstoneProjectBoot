import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { LOGIN_PATH } from './auth-api.service';
import { AuthSessionService } from './auth-session.service';

/**
 * 401 = token missing/expired/invalid: drop the session and go to /login.
 * 403 = signed in but the role is not allowed: keep the session; the page shows the
 * Problem Details message. A failed login (401 on the login call itself) stays on the form.
 */
export const unauthorizedInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(AuthSessionService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.endsWith(LOGIN_PATH)) {
        session.logout();
        void router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};
