import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthApiService } from '../../core/auth/auth-api.service';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  let auth: jasmine.SpyObj<AuthApiService>;
  let router: Router;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthApiService>('AuthApiService', ['login']);
    TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [provideRouter([]), { provide: AuthApiService, useValue: auth }],
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  it('signs in and lands on the home screen', () => {
    auth.login.and.returnValue(
      of({ accessToken: 't', tokenType: 'Bearer', expiresIn: 900, username: 'agent', roles: ['AGENT'] }),
    );
    const page = TestBed.createComponent(LoginPageComponent).componentInstance;
    page.username = ' agent ';
    page.password = 'pw';

    page.login();

    expect(auth.login).toHaveBeenCalledWith('agent', 'pw');
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
    expect(page.password).toBe('');
  });

  it('shows a generic message on bad credentials and clears the password', () => {
    auth.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const fixture = TestBed.createComponent(LoginPageComponent);
    const page = fixture.componentInstance;
    page.username = 'agent';
    page.password = 'wrong';

    page.login();
    fixture.detectChanges();

    expect(page.error()).toBe('Invalid username or password.');
    expect(page.password).toBe('');
    expect(router.navigate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[data-testid="login-error"]').textContent).toContain(
      'Invalid username or password.',
    );
  });

  it('renders the XSS probe as text, not markup', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="xss-probe"]').textContent).toContain(
      '<img src=x onerror=alert(1)>',
    );
  });

  it('toggles password visibility without submitting the form', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const input = el.querySelector<HTMLInputElement>('[data-testid="login-password"]')!;
    const toggle = el.querySelector<HTMLButtonElement>('[data-testid="login-password-toggle"]')!;

    expect(toggle.type).toBe('button');
    expect(input.type).toBe('password');
    expect(toggle.getAttribute('aria-label')).toBe('Show password');
    expect(toggle.getAttribute('aria-pressed')).toBe('false');

    toggle.click();
    fixture.detectChanges();

    expect(input.type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Hide password');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');
    expect(auth.login).not.toHaveBeenCalled();

    toggle.click();
    fixture.detectChanges();

    expect(input.type).toBe('password');
    expect(toggle.getAttribute('aria-label')).toBe('Show password');
  });
});
