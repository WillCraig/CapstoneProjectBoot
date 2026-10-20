import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { InteractionApiService } from './interaction-api.service';
import { Interaction } from './interaction.model';

describe('InteractionApiService', () => {
  const base = `${environment.apiBaseUrl}/api/v1/interactions`;
  const sample: Interaction = {
    id: '3f18537f-28b9-45b8-ae72-7e7a33d55250',
    customerId: 'CUS-1001',
    interactionType: 'NOTE',
    summary: 'Requested address update',
    correlationId: 'lab-request-001',
    createdAt: '2026-10-20T12:00:00Z',
  };

  let service: InteractionApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InteractionApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('lists interactions with the customerId query parameter', () => {
    let rows: Interaction[] | undefined;
    service.list('CUS-1001').subscribe((result) => (rows = result));

    const req = controller.expectOne(`${base}?customerId=CUS-1001`);
    expect(req.request.method).toBe('GET');
    req.flush([sample]);

    expect(rows).toEqual([sample]);
  });

  it('creates an interaction with the typed request body', () => {
    const body = {
      customerId: 'CUS-1001',
      interactionType: 'NOTE' as const,
      summary: 'Requested address update',
      correlationId: 'lab-request-001',
    };
    let created: Interaction | undefined;
    service.create(body).subscribe((result) => (created = result));

    const req = controller.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(sample);

    expect(created).toEqual(sample);
  });
});
