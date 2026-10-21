import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { LAB_CORRELATION_ID } from '../../core/correlation.interceptor';
import { InteractionApiService } from './interaction-api.service';
import { InteractionType } from './interaction.model';

@Component({
  selector: 'app-interaction-create-page',
  standalone: true,
  imports: [FormsModule, RouterLink],
  styleUrl: './interaction-list-page.component.css',
  template: `
    <section class="interactions">
      <p class="eyebrow">Customer activity</p>
      <h2>Record an interaction</h2>

      @if (error()) {
        <p class="error" role="alert" data-testid="create-error">{{ error() }}</p>
      }

      <form class="interaction-form" (ngSubmit)="create()">
        <label>
          Customer ID
          <input name="customerId" [(ngModel)]="customerId" required data-testid="create-customer-id" />
        </label>
        <label>
          Interaction type
          <select name="interactionType" [(ngModel)]="interactionType" data-testid="create-type">
            @for (type of interactionTypes; track type) {
              <option [value]="type">{{ type }}</option>
            }
          </select>
        </label>
        <label>
          Summary
          <textarea
            name="summary"
            [(ngModel)]="summary"
            required
            maxlength="1024"
            rows="5"
            data-testid="create-summary"
          ></textarea>
        </label>
        <button type="submit" data-testid="create" [disabled]="saving()">
          {{ saving() ? 'Saving…' : 'Save interaction' }}
        </button>
      </form>
      <div class="page-actions">
        <a class="back-button" routerLink="/interactions" [queryParams]="{ customerId }">
          Back to interactions
        </a>
      </div>
    </section>
  `,
})
export class InteractionCreatePageComponent implements OnInit {
  private readonly api = inject(InteractionApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly interactionTypes: InteractionType[] = ['CALL', 'EMAIL', 'NOTE', 'MEETING'];
  customerId = 'CUS-1001';
  interactionType: InteractionType = 'NOTE';
  summary = 'Follow-up on billing question';
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);

  ngOnInit(): void {
    this.customerId = this.route.snapshot.queryParamMap.get('customerId') ?? this.customerId;
  }

  create(): void {
    this.saving.set(true);
    this.error.set(null);
    this.api
      .create({
        customerId: this.customerId.trim(),
        interactionType: this.interactionType,
        summary: this.summary.trim(),
        correlationId: LAB_CORRELATION_ID,
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          void this.router.navigate(['/interactions', created.id]);
        },
        error: (err) => {
          this.error.set(apiErrorMessage(err, 'Could not save this interaction'));
          this.saving.set(false);
        },
      });
  }
}
