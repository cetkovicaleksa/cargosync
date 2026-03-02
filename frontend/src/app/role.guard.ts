import { inject, Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from './auth/service/auth.service';

@Injectable({
  providedIn: 'root',
})
export class RoleGuard implements CanActivate {
  readonly authService = inject(AuthService);
  readonly router = inject(Router);

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const expectedAuthorities: string[] = route.data['authorities'] ?? [];

    if (!expectedAuthorities.length) return true;

    const ok = expectedAuthorities.every(a => this.authService.hasAuthority(a));
    if (ok) return true;

    this.router.navigate(['/']);
    return false;
  }
}
