import { Injectable } from '@angular/core';
import confetti from 'canvas-confetti';

/** Two bursts from the bottom corners. canvas-confetti skips these when reduced motion is on. */
export function celebrationBursts(): confetti.Options[] {
  const shared = { particleCount: 80, spread: 70, startVelocity: 55, disableForReducedMotion: true };
  return [
    { ...shared, angle: 60, origin: { x: 0, y: 0.8 } },
    { ...shared, angle: 120, origin: { x: 1, y: 0.8 } },
  ];
}

/**
 * Small wrapper around canvas-confetti so components can celebrate and specs can stub it.
 * confetti draws on one canvas attached to <body>, so it keeps playing across route changes.
 */
@Injectable({ providedIn: 'root' })
export class CelebrationService {
  /** Specs replace this so they do not paint. */
  burst: (options: confetti.Options) => Promise<undefined> | null = confetti;

  celebrate(): void {
    for (const options of celebrationBursts()) {
      void this.burst(options);
    }
    document.querySelectorAll('body > canvas').forEach((canvas) => {
      canvas.setAttribute('aria-hidden', 'true');
    });
  }
}
