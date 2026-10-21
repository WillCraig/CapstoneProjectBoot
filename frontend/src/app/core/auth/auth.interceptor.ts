import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  let headers = request.headers
    .set('X-Correlation-ID', 'lab-request-001');

  if (token) {
    headers = headers.set(
      'Authorization',
      token.startsWith('Bearer ') ? token : `Bearer ${token}`
    );
  }

  return next(request.clone({ headers }));
};
