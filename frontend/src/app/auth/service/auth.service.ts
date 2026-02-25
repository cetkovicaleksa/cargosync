import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { TokenResponseDto } from '../dto/token-response.dto';
import { LoginRequestDto } from '../dto/login-request.dto';
import { JwtHelperService } from '@auth0/angular-jwt';
import { User } from '../../shared/model/user.model';
import { AccountDto } from '../dto/account.dto';
import {DecodedToken} from '../model/decoded-token.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiHost}auth`;

  private isLoggedInSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();

  private meSubject = new BehaviorSubject<AccountDto | null>(null);
  public me$ = this.meSubject.asObservable();

  constructor() {
    this.loadUserFromToken();
    if (this.hasToken()) {
      this.refreshMe().subscribe({ error: () => void 0 });
    }
  }

  private readonly http = inject(HttpClient);

  login(loginRequest: LoginRequestDto): Observable<TokenResponseDto> {
    return this.http.post<TokenResponseDto>(`${this.apiUrl}/login`, loginRequest).pipe(
      tap({
        next: response => this.handleLoginResponse(response),
        error: err => this.handleLoginError(err)
      })
    );
  }

  private handleLoginResponse(response: TokenResponseDto) {
    if (response.accessToken && typeof window !== 'undefined') {
      this.setToken(response.accessToken);
      this.loadUserFromToken(response.accessToken);
      this.isLoggedInSubject.next(true);

      this.refreshMe().subscribe({ error: () => void 0 });
    }
  }

  private handleLoginError(err: HttpErrorResponse) {
    console.log('Login failed: ', err);
    if (err.status === 403) {
      // blocked manager maps to 403
    }
  }

  private hasToken(): boolean {
    if (typeof window !== 'undefined') {
      return !!localStorage.getItem('token');
    }
    return false;
  }

  setToken(token: string) {
    if (typeof window !== 'undefined') {
      localStorage.setItem('token', token);
    }
  }

  removeToken() {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('token');
    }
  }

  logout() {
    if (typeof window !== 'undefined') {
      this.removeToken();
      this.isLoggedInSubject.next(false);
      this.meSubject.next(null);
    }
  }

  getToken(): string | null {
    if (typeof window !== 'undefined' && localStorage) {
      return localStorage.getItem('token');
    }
    return null;
  }

  isLoggedIn(): boolean {
    return this.isLoggedInSubject.value;
  }

  getMe(): AccountDto | null {
    return this.meSubject.value;
  }

  isPasswordChangeRequired(): boolean {
    return !!this.meSubject.value?.requiresPasswordChange;
  }

  hasAuthority(authority: string): boolean {
    const list = this.meSubject.value?.authorities ?? [];
    return list.some(a => {
      if (typeof a === 'string') return a === authority;
      return a.authority === authority;
    });
  }


  private loadUserFromToken(_token?: string) {
    const token = _token ?? this.getToken();
    const jwtHelper = new JwtHelperService();

    if (!token || jwtHelper.isTokenExpired(token)) {
      if (token) {
        console.log('[AuthService] Token expired or invalid');
        this.removeToken();
      } else console.log('[AuthService] No token found');

      if (this.meSubject.value) this.meSubject.next(null);
      return;
    }


    let decodedToken: DecodedToken | null = jwtHelper.decodeToken(token);
    if (!decodedToken) {
      console.log('[AuthService] Failed to decode token');
      return;
    }
    const newMe: AccountDto = {
      authorities: decodedToken.authorities,
      requiresPasswordChange: decodedToken.requiresPasswordChange,
      username: decodedToken.sub,
      role: undefined,
      accountNonLocked: true,
      owner: {email: decodedToken.email, avatar: decodedToken.avatar}
    }
    this.meSubject.next(newMe);
  }

  whoAmI(): Observable<AccountDto> {
    return this.http.get<AccountDto>(`${this.apiUrl}/whoami`);
  }

  refreshMe(): Observable<AccountDto> {
    return this.whoAmI().pipe(
      tap(me => this.meSubject.next(me))
    );
  }

  changePassword(oldPassword: string, newPassword: string): Observable<void> {
    return this.http.request<void>('POST', `${this.apiUrl}/change-password`, {
      body: { oldPassword, newPassword },
    });
  }
}
