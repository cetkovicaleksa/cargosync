import { Routes } from '@angular/router';
import { HomeComponent } from './shared/home/home/home.component';

// paramaters:
//   path - url
//   component - component to render
//   canActivate: [AuthGuard, RoleGuard] - guards
//   data: {roles: [list of roles]} - roles allowed to access the route
export const routes: Routes = [
    {path: '', redirectTo: 'home', pathMatch: 'full'},
    {path: 'home', component: HomeComponent},


    {path: '**', redirectTo: 'home'},
];
