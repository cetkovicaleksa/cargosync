import { inject, Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class RoleGuard implements CanActivate {
  // readonly authService = inject(AuthService);
  readonly router = inject(Router);

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expectedRoles: string[] = route.data['roles'];
    const userRole = ''//this.authService.getUserRole();

    if (userRole && expectedRoles.includes(userRole)) {
      return true;
    }

    this.router.navigate(['/home']);
    return false;
  }
}
