import { HttpInterceptorFn } from '@angular/common/http';
import { isApiUrl } from './api-url';

export const LAB_CORRELATION_ID = 'lab-request-001';

/** Sends the stable Lab 48/49 correlation id on API calls unless one is already set. */
export const correlationInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiUrl(req.url) || req.headers.has('X-Correlation-ID')) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'X-Correlation-ID': LAB_CORRELATION_ID } }));
};
