import { Routes } from '@angular/router';
import { HomeComponent } from './shared/home/home/home.component';
import { AuthGuard } from './auth.guard';
import { RoleGuard } from './role.guard';

import { SignInComponent } from './auth/page/signin/signin.component';
import { ForcePasswordChangeComponent } from './auth/page/force-password-change/force-password-change.component';
import { RegisterComponent } from './auth/page/register/register.component';
import { ActivateComponent } from './auth/page/activate/activate.component';
import { ManagerListComponent } from './manager/page/manager-list/manager-list.component';
import { ProductListComponent } from './product/page/product-list/product-list.component';
import { CatalogueComponent } from './order/page/catalogue/catalogue.component';
import { OrderListComponent } from './order/page/order-list/order-list.component';
import { WarehouseListComponent } from './warehouse/page/warehouse-list/warehouse-list.component';

// paramaters:
//   path - url
//   component - component to render
//   canActivate: [AuthGuard, RoleGuard] - guards
//   data: {roles: [list of roles]} - roles allowed to access the route
export const routes: Routes = [
    {path: '', redirectTo: 'home', pathMatch: 'full'},
    {path: 'home', component: HomeComponent},

    { path: 'signin', component: SignInComponent },
    { path: 'register', component: RegisterComponent },
    { path: 'auth/activate', component: ActivateComponent },

    {
      path: 'password-change',
      component: ForcePasswordChangeComponent,
    },

    {
      path: 'product',
      component: ProductListComponent,
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['ROLE_MANAGER'] },
    },

    {
      path: 'managers',
      component: ManagerListComponent,
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['MANAGE_MANAGERS'] },
    },

    {
      path: 'catalogue',
      component: CatalogueComponent,
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['ROLE_CUSTOMER'] },
    },

    {
      path: 'orders',
      component: OrderListComponent,
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['ROLE_CUSTOMER'] },
    },

    {
      path: 'warehouses',
      component: WarehouseListComponent,
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['ROLE_MANAGER'] },
    },

    {
      path: 'warehouses/:id',
      loadComponent: () =>
        import('./warehouse/page/warehouse-overview/warehouse-overview.component').then(
          (m) => m.WarehouseOverviewComponent,
        ),
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['ROLE_MANAGER'] },
    },

    {
      path: 'factories',
      loadComponent: () => import('./factory/page/factory-list/factory-list.component').then((m) => m.FactoryListComponent),
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['ROLE_MANAGER'] },
    },

    {
      path: 'factories/overview',
      loadComponent: () => import('./factory/page/factory-overview/factory-overview.component').then((m) => m.FactoryOverviewComponent),
      canActivate: [AuthGuard, RoleGuard],
      data: { authorities: ['ROLE_MANAGER'] },
    },

    {path: '**', redirectTo: 'home'},
];
