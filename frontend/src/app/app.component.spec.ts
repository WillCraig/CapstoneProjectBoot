import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthSessionService, LoginResponse } from './core/auth/auth-session.service';

const agentLogin: LoginResponse = {
  accessToken: 'header.payload.signature',
  tokenType: 'Bearer',
  expiresIn: 900,
  username: 'agent',
  roles: ['AGENT'],
};

describe('AppComponent header', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('hides the navigation when signed out', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
    expect(fixture.nativeElement.querySelector('.brand').textContent).toContain('Northstar CRM');
  });

  it('shows the main links and logs out to /login', () => {
    TestBed.inject(AuthSessionService).start(agentLogin);
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;

    const links = Array.from(el.querySelectorAll('nav a')).map((a) => a.textContent?.trim());
    expect(links).toEqual(['Customers', 'New customer', 'Interactions', 'New interaction']);
    expect(el.querySelector('[data-testid="signed-in-user"]')?.textContent).toContain('agent (AGENT)');

    (el.querySelector('[data-testid="logout"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(TestBed.inject(AuthSessionService).accessToken()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(el.querySelector('nav')).toBeNull();
  });

  it('signs out and returns to /login once the token has expired', () => {
    const session = TestBed.inject(AuthSessionService);
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.returnValue(Promise.resolve(true));
    const start = Date.now();
    session.start(agentLogin, start);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    fixture.componentInstance.checkExpiry(start + 60_000);
    expect(navigate).not.toHaveBeenCalled();

    fixture.componentInstance.checkExpiry(start + 901_000);
    fixture.detectChanges();

    expect(session.accessToken()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
  });
});
