import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthSessionService, LoginResponse } from '../../core/auth/auth-session.service';
import { CelebrationService } from '../../core/celebration.service';
import { CustomerApiService } from './customer-api.service';
import { CustomerDetailPageComponent } from './customer-detail-page.component';
import { CustomerFormPageComponent } from './customer-form-page.component';
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
  let celebration: jasmine.SpyObj<CelebrationService>;

  function configure(routeId?: string) {
    return TestBed.configureTestingModule({
      imports: [CustomerDetailPageComponent, CustomerFormPageComponent],
      providers: [
        provideRouter([]),
        { provide: CustomerApiService, useValue: api },
        { provide: CelebrationService, useValue: celebration },
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
    celebration = jasmine.createSpyObj<CelebrationService>('CelebrationService', ['celebrate']);
  });

  it('detail page loads the profile and closes the customer', async () => {
    await configure('CUS-1001');
    router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(CustomerDetailPageComponent);
    fixture.detectChanges();

    expect(api.getById).toHaveBeenCalledWith('CUS-1001');
    expect(fixture.nativeElement.textContent).toContain('Amina Khan');

    const confirm = spyOn(window, 'confirm').and.returnValue(true);
    fixture.componentInstance.close();
    expect(confirm).toHaveBeenCalledWith('Close Amina Khan (CUS-1001)?');
    expect(api.delete).toHaveBeenCalledWith('CUS-1001');
    expect(celebration.celebrate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(['/customers']);
  });

  it('detail page does nothing when the close is not confirmed', async () => {
    await configure('CUS-1001');
    router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(CustomerDetailPageComponent);
    fixture.detectChanges();

    spyOn(window, 'confirm').and.returnValue(false);
    fixture.componentInstance.close();

    expect(api.delete).not.toHaveBeenCalled();
    expect(celebration.celebrate).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('detail page shows the error and skips confetti when the close is refused', async () => {
    api.delete.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })),
    );
    await configure('CUS-1001');
    router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(CustomerDetailPageComponent);
    fixture.detectChanges();

    spyOn(window, 'confirm').and.returnValue(true);
    fixture.componentInstance.close();

    expect(api.delete).toHaveBeenCalledWith('CUS-1001');
    expect(celebration.celebrate).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toBeTruthy();
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
