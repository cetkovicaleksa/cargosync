import { inject, Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  // readonly authService = inject(AuthService);
  readonly router = inject(Router);

  canActivate(): boolean {
    if (true/*this.authService.isLoggedIn()*/) {
      return true;
    } else {
      this.router.navigate(['/signin']);
      return false;
    }
  }
}
