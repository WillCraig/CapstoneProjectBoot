import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { CustomerApiService } from './customer-api.service';
import { Customer } from './customer.model';

describe('CustomerApiService', () => {
  const base = `${environment.apiBaseUrl}/api/v1/customers`;
  const sample: Customer = {
    customerId: 'CUS-1001',
    fullName: 'Amina Khan',
    status: 'ACTIVE',
    createdAt: '2026-10-20T12:00:00Z',
  };

  let service: CustomerApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CustomerApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('searches with the q query parameter and omits it when blank', () => {
    let rows: Customer[] | undefined;
    service.search('amina').subscribe((result) => (rows = result));

    const req = controller.expectOne(`${base}?q=amina`);
    expect(req.request.method).toBe('GET');
    req.flush([sample]);
    expect(rows).toEqual([sample]);

    service.search('  ').subscribe();
    const all = controller.expectOne(base);
    expect(all.request.method).toBe('GET');
    all.flush([]);
  });

  it('creates a customer with the typed request body', () => {
    const body = { fullName: 'Priya Nair', status: 'PROSPECT' as const };
    let created: Customer | undefined;
    service.create(body).subscribe((result) => (created = result));

    const req = controller.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(sample);

    expect(created).toEqual(sample);
  });

  it('gets, updates, and deletes one customer by public id', () => {
    service.getById(sample.customerId).subscribe();
    let req = controller.expectOne(`${base}/${sample.customerId}`);
    expect(req.request.method).toBe('GET');
    req.flush(sample);

    const update = { fullName: 'Amina Khan', status: 'CLOSED' as const };
    service.update(sample.customerId, update).subscribe();
    req = controller.expectOne(`${base}/${sample.customerId}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(update);
    req.flush({ ...sample, ...update });

    service.delete(sample.customerId).subscribe();
    req = controller.expectOne(`${base}/${sample.customerId}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
