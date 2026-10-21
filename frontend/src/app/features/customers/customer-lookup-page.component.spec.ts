import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { CustomerApiService } from './customer-api.service';
import { CustomerLookupPageComponent } from './customer-lookup-page.component';

describe('CustomerLookupPageComponent', () => {
  let fixture: ComponentFixture<CustomerLookupPageComponent>;
  let api: jasmine.SpyObj<CustomerApiService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj<CustomerApiService>('CustomerApiService', ['search']);
    api.search.and.returnValue(of([
      { customerId: 'CUS-1002', fullName: 'Ravi Singh', status: 'PROSPECT', createdAt: '' },
      { customerId: 'CUS-1001', fullName: 'Amina Khan', status: 'ACTIVE', createdAt: '' },
      { customerId: 'CUS-1003', fullName: 'Mina Patel', status: 'ACTIVE', createdAt: '' },
    ]));

    await TestBed.configureTestingModule({
      imports: [CustomerLookupPageComponent],
      providers: [provideRouter([]), { provide: CustomerApiService, useValue: api }],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerLookupPageComponent);
    fixture.detectChanges();
  });

  it('loads customers sorted by ID and filters by status', () => {
    const component = fixture.componentInstance;
    expect(api.search).toHaveBeenCalledWith('');
    expect(component.visibleCustomers().map((customer) => customer.customerId))
      .toEqual(['CUS-1001', 'CUS-1002', 'CUS-1003']);

    component.updateStatusFilter('ACTIVE');
    component.updateSortDirection('desc');
    fixture.detectChanges();

    expect(component.visibleCustomers().map((customer) => customer.customerId))
      .toEqual(['CUS-1003', 'CUS-1001']);
    expect(fixture.nativeElement.textContent).not.toContain('Ravi Singh');
  });

  it('searches the API by customer name or ID', () => {
    fixture.componentInstance.query = 'amina';
    fixture.componentInstance.reload();

    expect(api.search).toHaveBeenCalledWith('amina');
  });

  it('renders Profile and Edit links on each row for the e2e flow', () => {
    const rows: NodeListOf<HTMLElement> =
      fixture.nativeElement.querySelectorAll('[data-testid="customer-row"]');
    expect(rows.length).toBe(3);
    const profile = rows[0].querySelector('[data-testid="customer-profile"]');
    const edit = rows[0].querySelector('[data-testid="customer-edit"]');
    expect(profile?.getAttribute('href')).toBe('/customers/CUS-1001');
    expect(edit?.getAttribute('href')).toBe('/customers/CUS-1001/edit');
  });

  it('shows the Problem Details message when the search fails', () => {
    api.search.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 403, error: {} })),
    );
    fixture.componentInstance.reload();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="list-error"]').textContent).toContain(
      'Your role does not allow this action.',
    );
  });
});
