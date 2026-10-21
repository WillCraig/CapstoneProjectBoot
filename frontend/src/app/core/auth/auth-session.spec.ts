import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthApiService, LOGIN_PATH } from './auth-api.service';
import { AuthSessionService, LoginResponse } from './auth-session.service';
import { unauthorizedInterceptor } from './unauthorized.interceptor';

const agentLogin: LoginResponse = {
  accessToken: 'header.payload.signature',
  tokenType: 'Bearer',
  expiresIn: 900,
  username: 'agent',
  roles: ['AGENT'],
};

describe('auth session', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let session: AuthSessionService;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([unauthorizedInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    session = TestBed.inject(AuthSessionService);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  afterEach(() => controller.verify());

  it('login posts credentials and keeps the JWT in memory, not web storage', () => {
    TestBed.inject(AuthApiService).login('agent', 'pw').subscribe();
    const req = controller.expectOne(`${environment.apiBaseUrl}${LOGIN_PATH}`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'agent', password: 'pw' });
    req.flush(agentLogin);

    expect(session.accessToken()).toBe(agentLogin.accessToken);
    expect(session.isAuthenticated()).toBeTrue();
    expect(session.isAdmin()).toBeFalse();
    expect(JSON.stringify(localStorage)).not.toContain(agentLogin.accessToken);
    expect(JSON.stringify(sessionStorage)).not.toContain(agentLogin.accessToken);
  });

  it('treats an expired token as signed out', () => {
    session.start(agentLogin, 0);
    expect(session.isAuthenticated(899_000)).toBeTrue();
    expect(session.isAuthenticated(900_000)).toBeFalse();
    expect(session.accessToken()).toBeNull();
  });

  it('isAdmin follows the ADMIN role', () => {
    session.start({ ...agentLogin, username: 'admin', roles: ['AGENT', 'ADMIN'] });
    expect(session.isAdmin()).toBeTrue();
  });

  it('a 401 from the API drops the session and goes to /login', () => {
    session.start(agentLogin);
    http.get(`${environment.apiBaseUrl}/api/v1/customers`).subscribe({ error: () => {} });
    controller
      .expectOne(`${environment.apiBaseUrl}/api/v1/customers`)
      .flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(session.accessToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('a 403 keeps the session (signed in, just not allowed)', () => {
    session.start(agentLogin);
    let seen: HttpErrorResponse | undefined;
    http
      .delete(`${environment.apiBaseUrl}/api/v1/customers/CUS-1002`)
      .subscribe({ error: (e: HttpErrorResponse) => (seen = e) });
    controller
      .expectOne(`${environment.apiBaseUrl}/api/v1/customers/CUS-1002`)
      .flush({ status: 403 }, { status: 403, statusText: 'Forbidden' });

    expect(seen?.status).toBe(403);
    expect(session.accessToken()).toBe(agentLogin.accessToken);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('a failed login does not redirect away from the form', () => {
    TestBed.inject(AuthApiService).login('agent', 'wrong').subscribe({ error: () => {} });
    controller
      .expectOne(`${environment.apiBaseUrl}${LOGIN_PATH}`)
      .flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(session.accessToken()).toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
