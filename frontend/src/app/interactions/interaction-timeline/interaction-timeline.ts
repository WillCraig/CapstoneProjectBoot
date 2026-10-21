import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { InteractionsService } from '../../core/api/interactions.service';
import { Interaction } from '../../core/models/crm.models';

@Component({
  selector: 'app-interaction-timeline',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './interaction-timeline.html',
  styleUrl: './interaction-timeline.css',
})
export class InteractionTimeline implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly interactionsService = inject(InteractionsService);
  private readonly cdr = inject(ChangeDetectorRef);

  customerId = '';
  interactions: Interaction[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.customerId = this.route.snapshot.paramMap.get('id') ?? '';

    if (!this.customerId) {
      this.errorMessage = 'A customer ID is required to view interactions.';
      return;
    }

    this.loadInteractions();
  }

  loadInteractions(): void {
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
          this.interactions = [...items].sort(
            (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
          );
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Failed to load interaction timeline:', error);

          this.errorMessage =
            error.status === 404
              ? 'No interaction endpoint was found for this customer.'
              : error.status === 401
                ? 'You are not authorized to view interactions.'
                : 'Unable to load the interaction timeline.';

          this.cdr.detectChanges();
        },
      });
  }

  deleteInteraction(interaction: Interaction): void {
    const confirmed = window.confirm('Are you sure you want to delete this interaction?');

    if (!confirmed) {
      return;
    }

    this.interactionsService.delete(this.customerId, interaction.interactionId).subscribe({
      next: () => this.loadInteractions(),
      error: (error) => {
        console.error('Failed to delete interaction:', error);
        this.errorMessage = 'Unable to delete the interaction. Please try again.';
        this.cdr.detectChanges();
      },
    });
  }
}
