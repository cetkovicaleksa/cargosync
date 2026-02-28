import { Routes } from '@angular/router';
import { HomeComponent } from './shared/home/home/home.component';
import { AuthGuard } from './auth.guard';
import { RoleGuard } from './role.guard';

import { SignInComponent } from './auth/page/signin/signin.component';
import { ForcePasswordChangeComponent } from './auth/page/force-password-change/force-password-change.component';
import { ManagerListComponent } from './manager/page/manager-list/manager-list.component';
import { ProductListComponent } from './product/page/product-list/product-list.component';

// paramaters:
//   path - url
//   component - component to render
//   canActivate: [AuthGuard, RoleGuard] - guards
//   data: {roles: [list of roles]} - roles allowed to access the route
export const routes: Routes = [
    {path: '', redirectTo: 'home', pathMatch: 'full'},
    {path: 'home', component: HomeComponent},

    { path: 'signin', component: SignInComponent },

    {
      path: 'password-change',
      component: ForcePasswordChangeComponent,
    },

    {
      path: 'product',
      component: ProductListComponent,
      canActivate: [AuthGuard],
    },

    {
      path: 'managers',
      component: ManagerListComponent,
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['MANAGE_MANAGERS'] },
    },

    {path: '**', redirectTo: 'home'},
];
