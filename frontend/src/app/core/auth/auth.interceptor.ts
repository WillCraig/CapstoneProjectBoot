import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthSessionService } from './auth-session.service';

/**
 * Attaches the in-memory demo token to API calls only. In Lab 51 the session
 * token becomes a real JWT; this interceptor is the one place that formats it.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthSessionService).accessToken();

  if (!token || !req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  return next(req.clone({ setHeaders: { Authorization: authorization } }));
};
