import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { CustomerApiService } from '../customers/customer-api.service';
import { InteractionApiService } from './interaction-api.service';
import { InteractionListPageComponent } from './interaction-list-page.component';

describe('InteractionListPageComponent', () => {
  let fixture: ComponentFixture<InteractionListPageComponent>;
  let api: jasmine.SpyObj<InteractionApiService>;
  let customerApi: jasmine.SpyObj<CustomerApiService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj<InteractionApiService>('InteractionApiService', ['list', 'create']);
    api.list.and.returnValue(of([]));
    api.create.and.returnValue(of({
      id: 'INT-1',
      customerId: 'CUS-1001',
      interactionType: 'NOTE',
      summary: 'Follow-up',
      correlationId: 'lab-request-001',
      createdAt: '2026-10-20T12:00:00Z',
    }));
    customerApi = jasmine.createSpyObj<CustomerApiService>('CustomerApiService', ['list']);
    customerApi.list.and.returnValue(of([
      { customerId: 'CUS-1001', fullName: 'Amina Khan', status: 'ACTIVE', createdAt: '' },
      { customerId: 'CUS-1002', fullName: 'Ravi Singh', status: 'PROSPECT', createdAt: '' },
    ]));

    await TestBed.configureTestingModule({
      imports: [InteractionListPageComponent],
      providers: [
        provideRouter([]),
        { provide: InteractionApiService, useValue: api },
        { provide: CustomerApiService, useValue: customerApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InteractionListPageComponent);
    fixture.detectChanges();
  });

  it('loads the customer activity and shows the empty state', () => {
    expect(api.list).toHaveBeenCalledWith('CUS-1001');
    expect(customerApi.list).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('No interactions yet');
    expect(fixture.nativeElement.querySelector('datalist option').getAttribute('label'))
      .toBe('Amina Khan (ACTIVE)');
  });

  it('loads the selected customer activity and records notes for that customer', () => {
    const component = fixture.componentInstance;
    component.customerIdInput = 'CUS-1002';
    component.useCustomer();
    component.summary = '  Called about renewal  ';
    component.create();

    expect(api.list).toHaveBeenCalledWith('CUS-1002');
    expect(api.create).toHaveBeenCalledWith({
      customerId: 'CUS-1002',
      interactionType: 'NOTE',
      summary: 'Called about renewal',
      correlationId: 'lab-request-001',
    });
    expect(component.summary).toBe('');
  });

  it('shows the correlation id and switches customers with the demo buttons', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="correlation-id"]')?.textContent).toBe('lab-request-001');

    (el.querySelector('[data-testid="customer-ravi"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(api.list).toHaveBeenCalledWith('CUS-1002');
    expect(el.querySelector('[data-testid="selected-customer"]')?.textContent).toBe('CUS-1002');
  });

  it('does not record an empty note', () => {
    fixture.componentInstance.summary = '   ';
    fixture.componentInstance.create();
    fixture.detectChanges();

    expect(api.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[data-testid="list-error"]').textContent).toContain(
      'Write a note',
    );
  });

  it('shows the Problem Details message when the timeline fails to load', () => {
    api.list.and.returnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
            error: { title: 'Not Found', detail: 'Customer CUS-9999 was not found' },
          }),
      ),
    );
    fixture.componentInstance.customerIdInput = 'CUS-9999';
    fixture.componentInstance.useCustomer();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="list-error"]')?.textContent).toContain('CUS-9999');
    expect(el.querySelector('[data-testid="empty-state"]')).toBeNull();
    expect(fixture.componentInstance.loading()).toBeFalse();
  });
});
