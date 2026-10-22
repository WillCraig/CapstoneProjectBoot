import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import axe from 'axe-core';
import { of, throwError } from 'rxjs';
import { AppComponent } from './app.component';
import { routes } from './app.routes';
import { CelebrationService, celebrationBursts } from './core/celebration.service';
import { AuthApiService } from './core/auth/auth-api.service';
import { AuthSessionService, LoginResponse } from './core/auth/auth-session.service';
import { CustomerChoicesPageComponent } from './features/customers/customer-choices-page.component';
import { CustomerDetailPageComponent } from './features/customers/customer-detail-page.component';
import { CustomerFormPageComponent } from './features/customers/customer-form-page.component';
import { CustomerLookupPageComponent } from './features/customers/customer-lookup-page.component';
import { CustomerApiService } from './features/customers/customer-api.service';
import { Customer } from './features/customers/customer.model';
import { InteractionCreatePageComponent } from './features/interactions/interaction-create-page.component';
import { InteractionDetailPageComponent } from './features/interactions/interaction-detail-page.component';
import { InteractionListPageComponent } from './features/interactions/interaction-list-page.component';
import { InteractionApiService } from './features/interactions/interaction-api.service';
import { Interaction } from './features/interactions/interaction.model';
import { LoginPageComponent } from './features/login/login-page.component';

const agentLogin: LoginResponse = {
  accessToken: 'header.payload.signature',
  tokenType: 'Bearer',
  expiresIn: 900,
  username: 'agent',
  roles: ['AGENT'],
};

const amina: Customer = {
  customerId: 'CUS-1001',
  fullName: 'Amina Khan',
  status: 'ACTIVE',
  createdAt: '2026-10-20T12:00:00Z',
};

const sample: Interaction = {
  id: '3f18537f-28b9-45b8-ae72-7e7a33d55250',
  customerId: 'CUS-1001',
  interactionType: 'NOTE',
  summary: 'Requested address update',
  correlationId: 'lab-request-001',
  createdAt: '2026-10-20T12:00:00Z',
};

/** Landmark and document rules need the full app shell, which these page fixtures do not include. */
async function expectAccessible(root: Element): Promise<void> {
  const results = await axe.run(root as HTMLElement, {
    rules: {
      region: { enabled: false },
      'landmark-one-main': { enabled: false },
      'document-title': { enabled: false },
      'html-has-lang': { enabled: false },
    },
  });
  const failures = results.violations
    .filter((violation) => violation.impact === 'critical' || violation.impact === 'serious' || violation.impact === 'moderate')
    .map((violation) => `${violation.impact} ${violation.id}: ${violation.help}`);
  expect(failures).toEqual([]);
}

describe('accessibility', () => {
  it('gives every screen its own document title', () => {
    const titled = routes.filter((route) => route.component);
    const titles = titled.map((route) => route.title);
    expect(titles.every((title) => typeof title === 'string' && String(title).endsWith('| Northstar CRM'))).toBeTrue();
    expect(new Set(titles).size).toBe(titles.length);
  });

  it('updates the document title and focuses the page heading after navigation', async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter(routes), provideHttpClient()],
    }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    const router = TestBed.inject(Router);
    fixture.detectChanges();

    await router.navigateByUrl('/login');
    fixture.detectChanges();
    expect(document.title).toBe('Sign in | Northstar CRM');
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toContain('Sign in');

    TestBed.inject(AuthSessionService).start(agentLogin);
    expect(await router.navigateByUrl('/home')).toBeTrue();
    fixture.detectChanges();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(router.url).toBe('/home');
    expect(document.title).toBe('Home | Northstar CRM');
    const heading: HTMLElement = fixture.nativeElement.querySelector('h1');
    expect(heading.textContent).toContain('What would you like to do?');
    expect(heading.tabIndex).toBe(-1);
    expect(document.activeElement).toBe(heading);
  });

  it('asks confetti to respect reduced motion and hides its canvas', () => {
    expect(celebrationBursts().every((burst) => burst.disableForReducedMotion)).toBeTrue();

    TestBed.configureTestingModule({});
    const service = TestBed.inject(CelebrationService);
    service.burst = jasmine.createSpy('burst');
    const canvas = document.createElement('canvas');
    document.body.appendChild(canvas);

    service.celebrate();

    expect(service.burst).toHaveBeenCalledTimes(2);
    expect(canvas.getAttribute('aria-hidden')).toBe('true');
    canvas.remove();
  });

  describe('login', () => {
    it('uses one heading and connects a failed sign-in to both fields', async () => {
      const auth = jasmine.createSpyObj<AuthApiService>('AuthApiService', ['login']);
      auth.login.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
      await TestBed.configureTestingModule({
        imports: [LoginPageComponent],
        providers: [provideRouter([]), { provide: AuthApiService, useValue: auth }],
      }).compileComponents();
      const fixture = TestBed.createComponent(LoginPageComponent);
      fixture.componentInstance.username = 'agent';
      fixture.componentInstance.password = 'wrong';
      fixture.componentInstance.login();
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelectorAll('h1').length).toBe(1);
      expect(el.querySelector('label[for="login-username"]')).not.toBeNull();
      expect(el.querySelector('label[for="login-password"]')).not.toBeNull();
      expect(el.querySelector('#login-username')?.getAttribute('aria-invalid')).toBe('true');
      expect(el.querySelector('#login-password')?.getAttribute('aria-describedby')).toBe('login-error');
      expect(el.querySelector('#login-error')?.getAttribute('role')).toBe('alert');
      await expectAccessible(el);
    });
  });

  describe('home and customers', () => {
    it('keeps a single heading level on the home choices', async () => {
      await TestBed.configureTestingModule({
        imports: [CustomerChoicesPageComponent],
        providers: [provideRouter([])],
      }).compileComponents();
      const fixture = TestBed.createComponent(CustomerChoicesPageComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector('h1')?.textContent).toContain('What would you like to do?');
      expect(el.querySelectorAll('h2').length).toBe(3);
      expect(el.querySelector('h3')).toBeNull();
      await expectAccessible(el);
    });

    it('names the customer table and disambiguates Profile and Edit', async () => {
      const api = jasmine.createSpyObj<CustomerApiService>('CustomerApiService', ['search']);
      api.search.and.returnValue(of([amina]));
      await TestBed.configureTestingModule({
        imports: [CustomerLookupPageComponent],
        providers: [provideRouter([]), { provide: CustomerApiService, useValue: api }],
      }).compileComponents();
      const fixture = TestBed.createComponent(CustomerLookupPageComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector('h1')?.textContent).toContain('Look up a customer');
      expect(el.querySelector('caption')?.textContent).toContain('Customers');
      expect(Array.from(el.querySelectorAll('th')).every((th) => th.getAttribute('scope') === 'col')).toBeTrue();
      expect(el.querySelector('label[for="customer-search"]')).not.toBeNull();
      expect(el.querySelector('label[for="customer-status-filter"]')).not.toBeNull();
      expect(el.querySelector('[data-testid="customer-profile"]')?.textContent).toContain('Amina Khan');
      expect(el.querySelector('[data-testid="customer-edit"]')?.textContent).toContain('Edit');
      await expectAccessible(el);
    });

    it('keeps the profile wording the demo reads', async () => {
      const api = jasmine.createSpyObj<CustomerApiService>('CustomerApiService', ['getById', 'delete']);
      api.getById.and.returnValue(of(amina));
      await TestBed.configureTestingModule({
        imports: [CustomerDetailPageComponent],
        providers: [
          provideRouter([]),
          { provide: CustomerApiService, useValue: api },
          { provide: CelebrationService, useValue: jasmine.createSpyObj('CelebrationService', ['celebrate']) },
          {
            provide: ActivatedRoute,
            useValue: { snapshot: { paramMap: convertToParamMap({ id: 'CUS-1001' }), queryParamMap: convertToParamMap({}) } },
          },
        ],
      }).compileComponents();
      TestBed.inject(AuthSessionService).start(agentLogin);
      const fixture = TestBed.createComponent(CustomerDetailPageComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector('h1')?.textContent).toContain('Customer profile');
      expect(el.querySelector('[data-testid="close-requires-admin"]')?.textContent).toContain(
        'Only an ADMIN can close a customer.',
      );
      expect(el.querySelector('[data-testid="record-interaction"]')?.textContent).toContain('Record an interaction');
      await expectAccessible(el);
    });

    it('labels the customer form fields', async () => {
      await TestBed.configureTestingModule({
        imports: [CustomerFormPageComponent],
        providers: [
          provideRouter([]),
          { provide: CustomerApiService, useValue: jasmine.createSpyObj('CustomerApiService', ['create', 'getById', 'update']) },
          {
            provide: ActivatedRoute,
            useValue: { snapshot: { paramMap: convertToParamMap({}), queryParamMap: convertToParamMap({}) } },
          },
        ],
      }).compileComponents();
      const fixture = TestBed.createComponent(CustomerFormPageComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector('h1')?.textContent).toContain('Create new customer');
      expect(el.querySelector('label[for="full-name"]')).not.toBeNull();
      expect(el.querySelector('label[for="status"]')).not.toBeNull();
      expect(el.querySelector('#public-id')?.getAttribute('aria-describedby')).toBe('public-id-help');
      await expectAccessible(el);
    });
  });

  describe('interactions', () => {
    function routeProvider(id = sample.id) {
      return {
        provide: ActivatedRoute,
        useValue: {
          snapshot: {
            paramMap: convertToParamMap({ id }),
            queryParamMap: convertToParamMap({ customerId: 'CUS-1001' }),
          },
        },
      };
    }

    it('names repeated timeline controls and announces a recorded note', async () => {
      const api = jasmine.createSpyObj<InteractionApiService>('InteractionApiService', ['list', 'create', 'delete']);
      api.list.and.returnValue(of([sample]));
      api.create.and.returnValue(of(sample));
      const customers = jasmine.createSpyObj<CustomerApiService>('CustomerApiService', ['list']);
      customers.list.and.returnValue(of([amina]));
      await TestBed.configureTestingModule({
        imports: [InteractionListPageComponent],
        providers: [
          provideRouter([]),
          { provide: InteractionApiService, useValue: api },
          { provide: CustomerApiService, useValue: customers },
          routeProvider(),
        ],
      }).compileComponents();
      const fixture: ComponentFixture<InteractionListPageComponent> = TestBed.createComponent(InteractionListPageComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector('h1')?.textContent).toContain('Interactions');
      expect(el.querySelector('h2')?.textContent).toContain('Recent activity');
      expect(el.querySelector('[data-testid="interaction-detail"]')?.textContent).toContain('Requested address update');
      expect(el.querySelector('[data-testid="delete-interaction"]')?.textContent).toContain('Delete');
      expect(el.querySelector('label[for="summary"]')).not.toBeNull();

      fixture.componentInstance.summary = '   ';
      fixture.componentInstance.create();
      fixture.detectChanges();
      expect(el.querySelector('#summary')?.getAttribute('aria-invalid')).toBe('true');
      expect(el.querySelector('#interaction-list-error')?.getAttribute('role')).toBe('alert');

      fixture.componentInstance.summary = 'Called about renewal';
      fixture.componentInstance.create();
      fixture.detectChanges();
      const notice = el.querySelector('[data-testid="note-recorded"]');
      expect(notice?.textContent).toContain('Note recorded.');
      expect(notice?.getAttribute('role')).toBe('status');
      await expectAccessible(el);
    });

    it('associates each record-interaction control with a label', async () => {
      await TestBed.configureTestingModule({
        imports: [InteractionCreatePageComponent],
        providers: [
          provideRouter([]),
          { provide: InteractionApiService, useValue: jasmine.createSpyObj('InteractionApiService', ['create']) },
          routeProvider(),
        ],
      }).compileComponents();
      const fixture = TestBed.createComponent(InteractionCreatePageComponent);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector('h1')?.textContent).toContain('Record an interaction');
      expect(el.querySelector('label[for="create-customer-id"]')).not.toBeNull();
      expect(el.querySelector('label[for="create-interaction-type"]')).not.toBeNull();
      expect(el.querySelector('label[for="create-summary"]')).not.toBeNull();
      await expectAccessible(el);
    });

    it('announces a saved interaction detail', async () => {
      const api = jasmine.createSpyObj<InteractionApiService>('InteractionApiService', ['getById', 'update', 'delete']);
      api.getById.and.returnValue(of(sample));
      api.update.and.returnValue(of(sample));
      await TestBed.configureTestingModule({
        imports: [InteractionDetailPageComponent],
        providers: [provideRouter([]), { provide: InteractionApiService, useValue: api }, routeProvider()],
      }).compileComponents();
      const fixture = TestBed.createComponent(InteractionDetailPageComponent);
      fixture.detectChanges();
      fixture.componentInstance.save();
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement;

      expect(el.querySelector('h1')?.textContent).toContain('Interaction details');
      expect(el.querySelector('[data-testid="saved-message"]')?.getAttribute('role')).toBe('status');
      expect(el.querySelector('[data-testid="saved-message"]')?.textContent).toContain('Changes saved.');
      expect(el.querySelector('label[for="edit-summary"]')).not.toBeNull();
      await expectAccessible(el);
    });
  });
});
