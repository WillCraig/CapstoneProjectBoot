import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { isApiUrl } from '../api-url';
import { AuthSessionService } from './auth-session.service';

/** Attaches the in-memory JWT to first-party API calls only, never to other hosts. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthSessionService).accessToken();

  if (!token || !isApiUrl(req.url)) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
