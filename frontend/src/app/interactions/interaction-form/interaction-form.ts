import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import {
  CreateInteractionRequest,
  InteractionChannel,
  UpdateInteractionRequest,
} from '../../core/models/crm.models';
import { InteractionsService } from '../../core/api/interactions.service';

@Component({
  selector: 'app-interaction-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './interaction-form.html',
  styleUrl: './interaction-form.css',
})
export class InteractionForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly interactionsService = inject(InteractionsService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly channels: InteractionChannel[] = ['PHONE', 'EMAIL', 'CHAT'];

  customerId = '';
  interactionId: string | null = null;
  isEditMode = false;

  channel: InteractionChannel = 'PHONE';
  summary = '';

  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    console.log('INTERACTION FORM COMPONENT LOADED');

    this.customerId = this.route.snapshot.paramMap.get('id') ?? '';
    this.interactionId = this.route.snapshot.paramMap.get('interactionId');
    this.isEditMode = this.interactionId !== null;

    if (!this.customerId) {
      this.errorMessage = 'A customer ID is required.';
      return;
    }

    if (this.isEditMode) {
      if (!this.interactionId) {
        this.errorMessage = 'An interaction ID is required to edit.';
        return;
      }

      this.loadInteractionForEditing();
    }
  }

  private loadInteractionForEditing(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.interactionsService
      .list(this.customerId)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: (items) => {
          console.log('Edit form received items:', items);
          console.log('Looking for interaction:', this.interactionId);

          const interaction = items.find((item) => item.interactionId === this.interactionId);

          console.log('Found interaction:', interaction);

          if (!interaction) {
            this.errorMessage = 'Interaction not found.';
            this.cdr.detectChanges();
            return;
          }

          this.channel = interaction.channel;
          this.summary = interaction.summary;
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Failed to load interaction for editing:', error);

          this.errorMessage =
            error.status === 401
              ? 'You are not authorized to view this interaction.'
              : error.status === 404
                ? 'The customer or interaction was not found.'
                : 'Unable to load the interaction. Please try again.';

          this.cdr.detectChanges();
        },
      });
  }

  submit(): void {
    this.errorMessage = '';

    const trimmedSummary = this.summary.trim();

    if (!this.customerId) {
      this.errorMessage = 'A customer ID is required.';
      return;
    }

    if (!trimmedSummary) {
      this.errorMessage = 'Enter a summary for this interaction.';
      return;
    }

    if (trimmedSummary.length > 2000) {
      this.errorMessage = 'The summary cannot exceed 2,000 characters.';
      return;
    }

    this.loading = true;
    this.cdr.detectChanges();

    if (this.isEditMode && this.interactionId) {
      const request: UpdateInteractionRequest = {
        channel: this.channel,
        summary: trimmedSummary,
      };

      this.interactionsService
        .update(this.customerId, this.interactionId, request)
        .pipe(
          finalize(() => {
            this.loading = false;
            this.cdr.detectChanges();
          }),
        )
        .subscribe({
          next: () => this.returnToTimeline(),
          error: (error) => {
            console.error('Failed to update interaction:', error);
            this.handleSaveError(error, true);
            this.cdr.detectChanges();
          },
        });

      return;
    }

    const request: CreateInteractionRequest = {
      channel: this.channel,
      summary: trimmedSummary,
    };

    this.interactionsService
      .create(this.customerId, request)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.detectChanges();
        }),
      )
      .subscribe({
        next: () => this.returnToTimeline(),
        error: (error) => {
          console.error('Failed to create interaction:', error);
          this.handleSaveError(error, false);
          this.cdr.detectChanges();
        },
      });
  }

  private returnToTimeline(): void {
    this.router.navigate(['/customers', this.customerId, 'interactions']);
  }

  private handleSaveError(error: { status?: number }, editing: boolean): void {
    this.errorMessage =
      error.status === 400
        ? 'The interaction details are invalid. Review the channel and summary.'
        : error.status === 401
          ? 'You are not authorized. Please sign in and try again.'
          : error.status === 404
            ? 'The customer or interaction was not found.'
            : editing
              ? 'Could not update the interaction. Please try again.'
              : 'Could not record the interaction. Please try again.';
  }
}
