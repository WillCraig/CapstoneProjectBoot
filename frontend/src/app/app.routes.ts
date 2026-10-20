import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { CustomerChoicesPageComponent } from './features/customers/customer-choices-page.component';
import { CustomerLookupPageComponent } from './features/customers/customer-lookup-page.component';
import { CustomerManagePageComponent } from './features/customers/customer-manage-page.component';
import { InteractionListPageComponent } from './features/interactions/interaction-list-page.component';
import { LoginPageComponent } from './features/login/login-page.component';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  { path: 'home', component: CustomerChoicesPageComponent, canActivate: [authGuard] },
  { path: 'customers', component: CustomerLookupPageComponent, canActivate: [authGuard] },
  { path: 'customers/manage/:customerId', component: CustomerManagePageComponent, canActivate: [authGuard] },
  { path: 'customers/manage', component: CustomerManagePageComponent, canActivate: [authGuard] },
  { path: 'interactions', component: InteractionListPageComponent, canActivate: [authGuard] },
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: '**', redirectTo: 'home' },
];
