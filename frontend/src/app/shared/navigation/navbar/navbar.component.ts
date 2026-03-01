import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatAnchor, MatButton, MatIconButton } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../auth/service/auth.service';
import { ProtectedImageComponent } from "../../util/protected-image.component";

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
    ProtectedImageComponent
],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavBar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private readonly loggedIn = signal<boolean>(this.auth.isLoggedIn());
  private readonly canManageManagers = signal<boolean>(this.auth.hasAuthority('MANAGE_MANAGERS'));
  private readonly isManager = signal<boolean>(this.auth.hasAuthority('ROLE_MANAGER'));
  private readonly username = signal<string | null>(this.auth.getMe()?.username ?? null);
  private readonly avatar = signal<string | null>(this.auth.getMe()?.owner?.avatar ?? null);

  constructor() {
    this.auth.isLoggedIn$.subscribe((v) => this.loggedIn.set(v));
    this.auth.me$.subscribe((me) => {
      this.username.set(me?.username ?? null);
      this.avatar.set(me?.owner?.avatar ?? null);
      this.canManageManagers.set(this.auth.hasAuthority('MANAGE_MANAGERS'));
      this.isManager.set(this.auth.hasAuthority('ROLE_MANAGER'));
    });
  }

  readonly navItems = computed<NavItem[]>(() => [
    {
      key: '/home',
      label: 'Home',
      visible: () => true,
    },
    {
      key: '/product',
      label: 'Products',
      visible: () => this.loggedIn(),
    },
    {
      key: '/managers',
      label: 'Managers',
      visible: () => this.loggedIn() && this.canManageManagers(),
    },
    {
      key: '/factories',
      label: 'Factories',
      visible: () => this.loggedIn() && this.isManager(),
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

  get avatarSrc() {
    return this.avatar();
  }

  get avatarInitial() {
    const name = this.username()?.trim();
    return name ? name.charAt(0).toUpperCase() : '?';
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
