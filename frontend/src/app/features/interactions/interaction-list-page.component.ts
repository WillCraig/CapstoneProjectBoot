import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InteractionApiService } from './interaction-api.service';
import { Interaction } from './interaction.model';

@Component({
  selector: 'app-interaction-list-page',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './interaction-list-page.component.html',
  styleUrl: './interaction-list-page.component.css',
})
export class InteractionListPageComponent {
  private readonly api = inject(InteractionApiService);

  readonly customerId = 'CUS-1001';
  summary = 'Follow-up on billing question';
  readonly rows = signal<Interaction[]>([]);
  readonly error = signal<string | null>(null);

  constructor() {
    this.reload();
  }

  create(): void {
    this.error.set(null);
    this.api
      .create({
        customerId: this.customerId,
        interactionType: 'NOTE',
        summary: this.summary,
        correlationId: 'lab-request-001',
      })
      .subscribe({
        next: () => this.reload(),
        error: (err) => this.error.set(`Create failed (${err.status ?? 'network'})`),
      });
  }

  private reload(): void {
    this.api.list(this.customerId).subscribe({
      next: (rows) => this.rows.set(rows),
      error: (err) => this.error.set(`List failed (${err.status ?? 'network'})`),
    });
  }
}
