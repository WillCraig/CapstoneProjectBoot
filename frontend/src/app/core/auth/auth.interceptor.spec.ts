import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { LAB_CORRELATION_ID, correlationInterceptor } from '../correlation.interceptor';
import { AuthSessionService } from './auth-session.service';

const TEST_JWT = 'header.payload.signature';
import { authInterceptor } from './auth.interceptor';

describe('API interceptors', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let session: AuthSessionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([correlationInterceptor, authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    session = TestBed.inject(AuthSessionService);
  });

  afterEach(() => controller.verify());

  it('adds the JWT bearer and correlation headers to API requests after login', () => {
    session.start({ accessToken: TEST_JWT, tokenType: 'Bearer', expiresIn: 900, username: 'agent', roles: ['AGENT'] });

    http.get(`${environment.apiBaseUrl}/api/v1/interactions`).subscribe();
    const req = controller.expectOne(`${environment.apiBaseUrl}/api/v1/interactions`);

    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${TEST_JWT}`);
    expect(req.request.headers.get('X-Correlation-ID')).toBe(LAB_CORRELATION_ID);
    req.flush([]);
  });

  it('does not add Authorization before login', () => {
    http.get(`${environment.apiBaseUrl}/api/v1/interactions`).subscribe();
    const req = controller.expectOne(`${environment.apiBaseUrl}/api/v1/interactions`);

    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('does not leak the token or correlation id to non-API URLs', () => {
    session.start({ accessToken: TEST_JWT, tokenType: 'Bearer', expiresIn: 900, username: 'agent', roles: ['AGENT'] });

    http.get('https://example.invalid/status').subscribe();
    const req = controller.expectOne('https://example.invalid/status');

    expect(req.request.headers.has('Authorization')).toBeFalse();
    expect(req.request.headers.has('X-Correlation-ID')).toBeFalse();
    req.flush({});
  });
});
