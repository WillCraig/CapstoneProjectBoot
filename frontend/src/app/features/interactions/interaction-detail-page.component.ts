import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/api-error';
import { InteractionApiService } from './interaction-api.service';
import { Interaction, InteractionType } from './interaction.model';

@Component({
  selector: 'app-interaction-detail-page',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <section class="page narrow">
      <a routerLink="/interactions" [queryParams]="{ customerId: interaction()?.customerId }">
        ← Back to interactions
      </a>
      <h2>Interaction details</h2>

      @if (error()) {
        <p class="notice notice-error" role="alert" data-testid="detail-error">{{ error() }}</p>
      }

      @if (interaction(); as current) {
        <dl class="details">
          <div><dt>Customer</dt><dd data-testid="detail-customer">{{ current.customerId }}</dd></div>
          <div><dt>Created</dt><dd>{{ current.createdAt | date: 'medium' }}</dd></div>
          <div><dt>Correlation</dt><dd>{{ current.correlationId }}</dd></div>
          <div><dt>Interaction ID</dt><dd class="break-word">{{ current.id }}</dd></div>
        </dl>

        <form class="stack-form" (ngSubmit)="save()">
          <label>
            Interaction type
            <select name="interactionType" [(ngModel)]="interactionType" data-testid="edit-type">
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
              data-testid="edit-summary"
            ></textarea>
          </label>
          <div class="form-actions">
            <button type="submit" data-testid="save-interaction" [disabled]="saving()">
              {{ saving() ? 'Saving…' : 'Save changes' }}
            </button>
            <button
              type="button"
              class="danger"
              data-testid="delete-interaction"
              [disabled]="deleting()"
              (click)="remove()"
            >
              {{ deleting() ? 'Deleting…' : 'Delete interaction' }}
            </button>
          </div>
        </form>
        @if (saved()) {
          <p class="notice notice-success" data-testid="saved-message">Changes saved.</p>
        }
      } @else if (loading()) {
        <p class="notice">Loading…</p>
      }
    </section>
  `,
})
export class InteractionDetailPageComponent implements OnInit {
  private readonly api = inject(InteractionApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly interactionTypes: InteractionType[] = ['CALL', 'EMAIL', 'NOTE', 'MEETING'];
  readonly interaction = signal<Interaction | null>(null);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly saved = signal(false);
  interactionType: InteractionType = 'NOTE';
  summary = '';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error.set('No interaction ID was provided.');
      return;
    }
    this.loading.set(true);
    this.api.getById(id).subscribe({
      next: (interaction) => {
        this.apply(interaction);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not load this interaction'));
        this.loading.set(false);
      },
    });
  }

  save(): void {
    const current = this.interaction();
    if (!current) return;
    this.saving.set(true);
    this.saved.set(false);
    this.error.set(null);
    this.api
      .update(current.id, {
        interactionType: this.interactionType,
        summary: this.summary.trim(),
      })
      .subscribe({
        next: (updated) => {
          this.apply(updated);
          this.saved.set(true);
          this.saving.set(false);
        },
        error: (err) => {
          this.error.set(apiErrorMessage(err, 'Could not save these changes'));
          this.saving.set(false);
        },
      });
  }

  remove(): void {
    const current = this.interaction();
    if (!current) return;
    this.deleting.set(true);
    this.error.set(null);
    this.api.delete(current.id).subscribe({
      next: () => {
        this.deleting.set(false);
        void this.router.navigate(['/interactions'], {
          queryParams: { customerId: current.customerId },
        });
      },
      error: (err) => {
        this.error.set(apiErrorMessage(err, 'Could not delete this interaction'));
        this.deleting.set(false);
      },
    });
  }

  private apply(interaction: Interaction): void {
    this.interaction.set(interaction);
    this.interactionType = interaction.interactionType;
    this.summary = interaction.summary;
  }
}
