import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'interactions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/interactions/interaction-list-page.component').then(
        (m) => m.InteractionListPageComponent,
      ),
  },
  {
    path: 'interactions/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/interactions/interaction-create-page.component').then(
        (m) => m.InteractionCreatePageComponent,
      ),
  },
  {
    path: 'interactions/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/interactions/interaction-detail-page.component').then(
        (m) => m.InteractionDetailPageComponent,
      ),
  },
  { path: '', pathMatch: 'full', redirectTo: 'interactions' },
  { path: '**', redirectTo: 'interactions' },
];
