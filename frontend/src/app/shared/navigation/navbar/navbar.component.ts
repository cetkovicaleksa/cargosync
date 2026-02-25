import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatAnchor, MatButton, MatIconButton } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../auth/service/auth.service';

type NavItem = { key: string; label: string; visible: () => boolean };

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLink,
    RouterLinkActive,
    MatAnchor,
    MatButton,
    MatIconButton,
    MatIconModule,
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavBar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private readonly loggedIn = signal<boolean>(this.auth.isLoggedIn());
  private readonly canManageManagers = signal<boolean>(this.auth.hasAuthority('MANAGE_MANAGERS'));
  private readonly username = signal<string | null>(this.auth.getMe()?.username ?? null);

  constructor() {
    this.auth.isLoggedIn$.subscribe((v) => this.loggedIn.set(v));
    this.auth.me$.subscribe((me) => {
      this.username.set(me?.username ?? null);
      this.canManageManagers.set(this.auth.hasAuthority('MANAGE_MANAGERS'));
    });
  }

  readonly navItems = computed<NavItem[]>(() => [
    {
      key: '/home',
      label: 'Home',
      visible: () => true,
    },
    {
      key: '/managers',
      label: 'Managers',
      visible: () => this.loggedIn() && this.canManageManagers(),
    },
  ]);

  get items() {
    return this.navItems().filter((x) => x.visible());
  }

  get isLoggedIn() {
    return this.loggedIn();
  }

  get displayName() {
    return this.username();
  }

  goToPasswordChange() {
    this.router.navigate(['/password-change']);
  }

  signIn() {
    this.router.navigate(['/signin']);
  }

  signOut() {
    this.auth.logout();
    this.router.navigate(['/signin']);
  }
}
