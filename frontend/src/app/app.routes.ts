import { Routes } from '@angular/router';
import { HomeComponent } from './shared/home/home/home.component';
import {AuthGuard} from './auth.guard';
import {RoleGuard} from './role.guard';
import {PasswordChangeGuard} from './auth/guard/password-change.guard';

import { SignInComponent } from './auth/pages/signin/signin.component';
import { ForcePasswordChangeComponent } from './auth/pages/force-password-change/force-password-change.component';
import { ManagerListComponent } from './managers/pages/manager-list/manager-list.component';
import { ManagerCreateComponent } from './managers/pages/manager-create/manager-create.component';

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
      path: 'force-password-change',
      component: ForcePasswordChangeComponent,
      canActivate: [AuthGuard],
    },

    {
      path: 'managers',
      canActivate: [AuthGuard, PasswordChangeGuard, RoleGuard],
      data: { authorities: ['MANAGE_MANAGERS'] },
      children: [
        { path: '', component: ManagerListComponent },
        { path: 'new', component: ManagerCreateComponent },
      ],
    },

    {path: '**', redirectTo: 'home'},
];
