import { Injectable } from '@angular/core';
import confetti from 'canvas-confetti';

/**
 * Small wrapper around canvas-confetti so components can celebrate and specs can stub it.
 * confetti draws on one canvas attached to <body>, so it keeps playing across route changes.
 */
@Injectable({ providedIn: 'root' })
export class CelebrationService {
  /** Two bursts from the bottom corners. Skipped when the user prefers reduced motion. */
  celebrate(): void {
    const shared = { particleCount: 80, spread: 70, startVelocity: 55, disableForReducedMotion: true };
    void confetti({ ...shared, angle: 60, origin: { x: 0, y: 0.8 } });
    void confetti({ ...shared, angle: 120, origin: { x: 1, y: 0.8 } });
  }
}
