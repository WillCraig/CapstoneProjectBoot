import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'customers',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customers/customer-list-page.component').then(
        (m) => m.CustomerListPageComponent,
      ),
  },
  {
    path: 'customers/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customers/customer-form-page.component').then(
        (m) => m.CustomerFormPageComponent,
      ),
  },
  {
    path: 'customers/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customers/customer-detail-page.component').then(
        (m) => m.CustomerDetailPageComponent,
      ),
  },
  {
    path: 'customers/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/customers/customer-form-page.component').then(
        (m) => m.CustomerFormPageComponent,
      ),
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
  { path: '', pathMatch: 'full', redirectTo: 'customers' },
  { path: '**', redirectTo: 'customers' },
];
