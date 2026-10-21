import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { CustomerChoicesPageComponent } from './features/customers/customer-choices-page.component';
import { CustomerDetailPageComponent } from './features/customers/customer-detail-page.component';
import { CustomerFormPageComponent } from './features/customers/customer-form-page.component';
import { CustomerLookupPageComponent } from './features/customers/customer-lookup-page.component';
import { CustomerManagePageComponent } from './features/customers/customer-manage-page.component';
import { InteractionCreatePageComponent } from './features/interactions/interaction-create-page.component';
import { InteractionDetailPageComponent } from './features/interactions/interaction-detail-page.component';
import { InteractionListPageComponent } from './features/interactions/interaction-list-page.component';
import { LoginPageComponent } from './features/login/login-page.component';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'home', component: CustomerChoicesPageComponent, canActivate: [authGuard] },
  { path: 'customers', component: CustomerLookupPageComponent, canActivate: [authGuard] },
  { path: 'customers/new', component: CustomerFormPageComponent, canActivate: [authGuard] },
  { path: 'customers/:id/edit', component: CustomerFormPageComponent, canActivate: [authGuard] },
  { path: 'customers/manage', component: CustomerManagePageComponent, canActivate: [authGuard] },
  {
    path: 'customers/manage/:customerId',
    component: CustomerManagePageComponent,
    canActivate: [authGuard],
  },
  { path: 'customers/:id', component: CustomerDetailPageComponent, canActivate: [authGuard] },
  { path: 'interactions', component: InteractionListPageComponent, canActivate: [authGuard] },
  {
    path: 'interactions/new',
    component: InteractionCreatePageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'interactions/:id',
    component: InteractionDetailPageComponent,
    canActivate: [authGuard],
  },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: '**', redirectTo: 'home' },
];
