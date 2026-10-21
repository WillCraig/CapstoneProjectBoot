import { Routes } from '@angular/router';

import { CustomerSearch } from './customers/customer-search/customer-search';
import { CustomerProfile } from './customers/customer-profile/customer-profile';
import { InteractionTimeline } from './interactions/interaction-timeline/interaction-timeline';
import { InteractionForm } from './interactions/interaction-form/interaction-form';

export const routes: Routes = [
  { path: '', redirectTo: 'customers', pathMatch: 'full' },
  { path: 'customers', component: CustomerSearch },
  {
    path: 'customers/:id/interactions/new',
    component: InteractionForm,
  },

  {
    path: 'customers/:id/interactions/:interactionId/edit',
    component: InteractionForm,
  },

  {
    path: 'customers/:id/interactions',
    component: InteractionTimeline,
  },
  { path: 'customers/:id', component: CustomerProfile },
  { path: '**', redirectTo: 'customers' },
];
