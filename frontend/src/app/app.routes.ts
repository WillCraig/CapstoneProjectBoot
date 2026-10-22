import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { CustomerChoicesPageComponent } from './features/customers/customer-choices-page.component';
import { CustomerDetailPageComponent } from './features/customers/customer-detail-page.component';
import { CustomerFormPageComponent } from './features/customers/customer-form-page.component';
import { CustomerLookupPageComponent } from './features/customers/customer-lookup-page.component';
import { InteractionCreatePageComponent } from './features/interactions/interaction-create-page.component';
import { InteractionDetailPageComponent } from './features/interactions/interaction-detail-page.component';
import { InteractionListPageComponent } from './features/interactions/interaction-list-page.component';
import { LoginPageComponent } from './features/login/login-page.component';

const site = 'Northstar CRM';

export const routes: Routes = [
  { path: 'login', title: `Sign in | ${site}`, component: LoginPageComponent },
  { path: 'home', title: `Home | ${site}`, component: CustomerChoicesPageComponent, canActivate: [authGuard] },
  { path: 'customers', title: `Look up a customer | ${site}`, component: CustomerLookupPageComponent, canActivate: [authGuard] },
  { path: 'customers/new', title: `Create new customer | ${site}`, component: CustomerFormPageComponent, canActivate: [authGuard] },
  { path: 'customers/:id/edit', title: `Update customer | ${site}`, component: CustomerFormPageComponent, canActivate: [authGuard] },
  { path: 'customers/:id', title: `Customer profile | ${site}`, component: CustomerDetailPageComponent, canActivate: [authGuard] },
  { path: 'interactions', title: `Interactions | ${site}`, component: InteractionListPageComponent, canActivate: [authGuard] },
  {
    path: 'interactions/new',
    title: `Record an interaction | ${site}`,
    component: InteractionCreatePageComponent,
    canActivate: [authGuard],
  },
  {
    path: 'interactions/:id',
    title: `Interaction details | ${site}`,
    component: InteractionDetailPageComponent,
    canActivate: [authGuard],
  },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: '**', redirectTo: 'home' },
];
