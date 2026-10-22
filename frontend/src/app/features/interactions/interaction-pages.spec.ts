import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { InteractionCreatePageComponent } from './interaction-create-page.component';
import { InteractionDetailPageComponent } from './interaction-detail-page.component';
import { InteractionListPageComponent } from './interaction-list-page.component';
import { InteractionApiService } from './interaction-api.service';
import { Interaction } from './interaction.model';
import { CustomerApiService } from '../customers/customer-api.service';

const sample: Interaction = {
  id: '3f18537f-28b9-45b8-ae72-7e7a33d55250',
  customerId: 'CUS-1001',
  interactionType: 'NOTE',
  summary: 'Requested address update',
  correlationId: 'lab-request-001',
  createdAt: '2026-10-20T12:00:00Z',
};

describe('interaction pages', () => {
  let api: jasmine.SpyObj<InteractionApiService>;
  let router: Router;

  beforeEach(async () => {
    api = jasmine.createSpyObj<InteractionApiService>('InteractionApiService', [
      'list',
      'create',
      'getById',
      'update',
      'delete',
    ]);
    api.list.and.returnValue(of([sample]));
    api.create.and.returnValue(of(sample));
    api.getById.and.returnValue(of(sample));
    api.update.and.returnValue(of({ ...sample, interactionType: 'EMAIL', summary: 'Updated' }));
    api.delete.and.returnValue(of(undefined));

    await TestBed.configureTestingModule({
      imports: [
        InteractionListPageComponent,
        InteractionCreatePageComponent,
        InteractionDetailPageComponent,
      ],
      providers: [
        provideRouter([]),
        { provide: InteractionApiService, useValue: api },
        { provide: CustomerApiService, useValue: { list: () => of([]) } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ customerId: 'CUS-1001' }),
              paramMap: convertToParamMap({ id: sample.id }),
            },
          },
        },
      ],
    }).compileComponents();
    router = TestBed.inject(Router);
  });

  it('list page loads and renders Amina timeline rows', () => {
    const fixture: ComponentFixture<InteractionListPageComponent> =
      TestBed.createComponent(InteractionListPageComponent);
    fixture.detectChanges();

    expect(api.list).toHaveBeenCalledWith('CUS-1001');
    expect(fixture.nativeElement.textContent).toContain('Requested address update');
  });

  it('list page deletes a row only after the user confirms', () => {
    const fixture: ComponentFixture<InteractionListPageComponent> =
      TestBed.createComponent(InteractionListPageComponent);
    fixture.detectChanges();
    const confirm = spyOn(window, 'confirm').and.returnValue(false);

    fixture.componentInstance.remove(sample);
    expect(confirm).toHaveBeenCalledWith(`Delete "${sample.summary}" for CUS-1001?`);
    expect(api.delete).not.toHaveBeenCalled();

    confirm.and.returnValue(true);
    fixture.componentInstance.remove(sample);
    expect(api.delete).toHaveBeenCalledWith(sample.id);
  });

  it('detail page keeps the interaction when the delete is not confirmed', () => {
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(InteractionDetailPageComponent);
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(false);

    fixture.componentInstance.remove();

    expect(api.delete).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('create page submits the Lab 49 DTO and routes to the created detail', () => {
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(InteractionCreatePageComponent);
    fixture.detectChanges();
    fixture.componentInstance.summary = 'A new note';

    fixture.componentInstance.create();

    expect(api.create).toHaveBeenCalledWith({
      customerId: 'CUS-1001',
      interactionType: 'NOTE',
      summary: 'A new note',
      correlationId: 'lab-request-001',
    });
    expect(navigate).toHaveBeenCalledWith(['/interactions', sample.id]);
  });

  it('detail page loads, updates, and deletes an interaction', () => {
    const navigate = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const fixture = TestBed.createComponent(InteractionDetailPageComponent);
    fixture.detectChanges();

    expect(api.getById).toHaveBeenCalledWith(sample.id);
    fixture.componentInstance.interactionType = 'EMAIL';
    fixture.componentInstance.summary = 'Updated';
    fixture.componentInstance.save();
    expect(api.update).toHaveBeenCalledWith(sample.id, {
      interactionType: 'EMAIL',
      summary: 'Updated',
    });

    const confirm = spyOn(window, 'confirm').and.returnValue(true);
    fixture.componentInstance.remove();
    expect(confirm).toHaveBeenCalledWith('Delete "Updated" for CUS-1001?');
    expect(api.delete).toHaveBeenCalledWith(sample.id);
    expect(navigate).toHaveBeenCalledWith(['/interactions'], {
      queryParams: { customerId: 'CUS-1001' },
    });
  });
});
