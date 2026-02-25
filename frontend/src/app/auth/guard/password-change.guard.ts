import { inject, Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../service/auth.service';

@Injectable({ providedIn: 'root' })
export class PasswordChangeGuard implements CanActivate {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  canActivate(): boolean {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/signin']);
      return false;
    }

    if (this.auth.isPasswordChangeRequired()) {
      this.router.navigate(['/force-password-change']);
      return false;
    }

    return true;
  }
}
