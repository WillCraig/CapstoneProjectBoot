import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CustomerApiService } from './customer-api.service';
import { CustomerLookupPageComponent } from './customer-lookup-page.component';

describe('CustomerLookupPageComponent', () => {
  let fixture: ComponentFixture<CustomerLookupPageComponent>;
  let api: jasmine.SpyObj<CustomerApiService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj<CustomerApiService>('CustomerApiService', ['list']);
    api.list.and.returnValue(of([
      { customerId: 'CUS-1002', fullName: 'Ravi Singh', status: 'PROSPECT' },
      { customerId: 'CUS-1001', fullName: 'Amina Khan', status: 'ACTIVE' },
      { customerId: 'CUS-1003', fullName: 'Mina Patel', status: 'ACTIVE' },
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
    expect(api.list).toHaveBeenCalled();
    expect(component.visibleCustomers().map((customer) => customer.customerId))
      .toEqual(['CUS-1001', 'CUS-1002', 'CUS-1003']);

    component.updateStatusFilter('ACTIVE');
    component.updateSortDirection('desc');
    fixture.detectChanges();

    expect(component.visibleCustomers().map((customer) => customer.customerId))
      .toEqual(['CUS-1003', 'CUS-1001']);
    expect(fixture.nativeElement.textContent).not.toContain('Ravi Singh');
  });
});
