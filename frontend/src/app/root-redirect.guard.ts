import { inject, Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth/service/auth.service';

export const rootRedirectGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const isLoggedIn = authService.isLoggedIn();
    const userAuthorities = authService.getMe()?.authorities ?? [];

    if (!isLoggedIn) {
        return router.parseUrl('/signin');
    } else if (userAuthorities.includes('ROLE_MANAGER')) {
        return router.parseUrl('/product');
    } else if (userAuthorities.includes('ROLE_CUSTOMER')) {
        return router.parseUrl('/catalogue');
    }

    return router.parseUrl('/signin');
}
