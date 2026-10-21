import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthSessionService, LoginResponse } from '../../core/auth/auth-session.service';
import { CustomerApiService } from './customer-api.service';
import { CustomerDetailPageComponent } from './customer-detail-page.component';
import { CustomerFormPageComponent } from './customer-form-page.component';
import { CustomerListPageComponent } from './customer-list-page.component';
import { Customer } from './customer.model';

const amina: Customer = {
  customerId: 'CUS-1001',
  fullName: 'Amina Khan',
  status: 'ACTIVE',
  createdAt: '2026-10-20T12:00:00Z',
};
const ravi: Customer = {
  customerId: 'CUS-1002',
  fullName: 'Ravi Singh',
  status: 'PROSPECT',
  createdAt: '2026-10-20T12:00:00Z',
};
const agentLogin: LoginResponse = {
  accessToken: 'header.payload.signature',
  tokenType: 'Bearer',
  expiresIn: 900,
  username: 'agent',
  roles: ['AGENT'],
};
const adminLogin: LoginResponse = { ...agentLogin, username: 'admin', roles: ['AGENT', 'ADMIN'] };

function statusOptionsIn(fixture: ComponentFixture<CustomerFormPageComponent>): string[] {
  const select: HTMLSelectElement = fixture.nativeElement.querySelector(
    '[data-testid="customer-status"]',
  );
  return Array.from(select.options).map((option) => option.value);
}

describe('customer pages', () => {
  let api: jasmine.SpyObj<CustomerApiService>;
  let router: Router;

  function configure(routeId?: string) {
    return TestBed.configureTestingModule({
      imports: [
        CustomerListPageComponent,
        CustomerDetailPageComponent,
        CustomerFormPageComponent,
      ],
      providers: [
        provideRouter([]),
        { provide: CustomerApiService, useValue: api },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({}),
              paramMap: convertToParamMap(routeId ? { id: routeId } : {}),
            },
          },
        },
      ],
    }).compileComponents();
  }

  beforeEach(() => {
    api = jasmine.createSpyObj<CustomerApiService>('CustomerApiService', [
      'search',
      'create',
      'getById',
      'update',
      'delete',
    ]);
    api.search.and.returnValue(of([amina, ravi]));
    api.create.and.returnValue(of({ ...ravi, customerId: 'CUS-1003', fullName: 'Priya Nair' }));
    api.getById.and.returnValue(of(amina));
    api.update.and.returnValue(of({ ...amina, status: 'CLOSED' }));
    api.delete.and.returnValue(of(undefined));
  });

  it('list page loads all customers and renders profile rows', async () => {
    await configure();
    router = TestBed.inject(Router);
    const fixture: ComponentFixture<CustomerListPageComponent> =
      TestBed.createComponent(CustomerListPageComponent);
    fixture.detectChanges();

    expect(api.search).toHaveBeenCalledWith('');
    expect(fixture.nativeElement.textContent).toContain('Amina Khan');
    expect(fixture.nativeElement.textContent).toContain('Ravi Singh');
    expect(router).toBeTruthy();
  });

  it('list page searches by the typed term', async () => {
    await configure();
    const fixture = TestBed.createComponent(CustomerListPageComponent);
    fixture.detectChanges();
    fixture.componentInstance.query = 'amina';

    fixture.componentInstance.reload();

    expect(api.search).toHaveBeenCalledWith('amina');
  });

  it('detail page loads the profile and closes the customer', async () => {
    await configure('CUS-1001');
    router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(CustomerDetailPageComponent);
    fixture.detectChanges();

    expect(api.getById).toHaveBeenCalledWith('CUS-1001');
    expect(fixture.nativeElement.textContent).toContain('Amina Khan');

    fixture.componentInstance.close();
    expect(api.delete).toHaveBeenCalledWith('CUS-1001');
    expect(navigate).toHaveBeenCalledWith(['/customers']);
  });

  it('form page without an id creates and routes to the new profile', async () => {
    await configure();
    router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(CustomerFormPageComponent);
    fixture.detectChanges();
    fixture.componentInstance.fullName = ' Priya Nair ';
    fixture.componentInstance.status = 'PROSPECT';

    fixture.componentInstance.save();

    expect(api.create).toHaveBeenCalledWith({ fullName: 'Priya Nair', status: 'PROSPECT' });
    expect(navigate).toHaveBeenCalledWith(['/customers', 'CUS-1003']);
  });

  it('edit form hides CLOSED from an agent', async () => {
    await configure('CUS-1001');
    TestBed.inject(AuthSessionService).start(agentLogin);
    const fixture = TestBed.createComponent(CustomerFormPageComponent);
    fixture.detectChanges();

    expect(statusOptionsIn(fixture)).toEqual(['PROSPECT', 'ACTIVE']);
  });

  it('edit form offers an agent no way to reopen a CLOSED customer', async () => {
    api.getById.and.returnValue(of({ ...amina, status: 'CLOSED' }));
    await configure('CUS-1001');
    TestBed.inject(AuthSessionService).start(agentLogin);
    const fixture = TestBed.createComponent(CustomerFormPageComponent);
    fixture.detectChanges();

    expect(statusOptionsIn(fixture)).toEqual(['CLOSED']);
  });

  it('edit form offers CLOSED to an admin', async () => {
    await configure('CUS-1001');
    TestBed.inject(AuthSessionService).start(adminLogin);
    const fixture = TestBed.createComponent(CustomerFormPageComponent);
    fixture.detectChanges();

    expect(statusOptionsIn(fixture)).toEqual(['PROSPECT', 'ACTIVE', 'CLOSED']);
  });

  it('form page with an id loads, updates, and routes back to the profile', async () => {
    await configure('CUS-1001');
    TestBed.inject(AuthSessionService).start(adminLogin);
    router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(CustomerFormPageComponent);
    fixture.detectChanges();

    expect(api.getById).toHaveBeenCalledWith('CUS-1001');
    expect(fixture.componentInstance.fullName).toBe('Amina Khan');
    fixture.componentInstance.status = 'CLOSED';

    fixture.componentInstance.save();

    expect(api.update).toHaveBeenCalledWith('CUS-1001', {
      fullName: 'Amina Khan',
      status: 'CLOSED',
    });
    expect(navigate).toHaveBeenCalledWith(['/customers', 'CUS-1001']);
  });
});
