import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { AccountAuthority } from '../dto/account-authority.type';
import { TokenResponseDto } from '../dto/token-response.dto';
import { LoginRequestDto } from '../dto/login-request.dto';
import { JwtHelperService } from '@auth0/angular-jwt';
import { User } from '../../shared/model/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = `${environment.apiHost}auth`
  private isLoggedInSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isLoggedIn$ = this.isLoggedInSubject.asObservable();
  private userSubject = new BehaviorSubject<User | null>(null);
  public user$ = this.userSubject.asObservable();

  constructor() {
    this.loadUserFromToken();
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
    }
  }

  private handleLoginError(err: HttpErrorResponse) {
    console.log('Login failed: ', err);
    if (err.status === 403) {
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
      this.userSubject.next(null);
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

  getUser(): User | null {
    return this.userSubject.value;
  }

  private loadUserFromToken(_token?: string) {
    const token = _token ?? this.getToken();
    const jwtHelper = new JwtHelperService();

    if (!token || jwtHelper.isTokenExpired(token)) {
      if (token) {
        console.log('[AuthService] Token expired or invalid');
        this.removeToken();
      }
      else console.log('[AuthService] No token found');

      if (this.userSubject.value)
        this.userSubject.next(null);

      return;
    }

    const decoded = jwtHelper.decodeToken(token);
    console.log('[AuthService] Decoded token:', decoded);
    const user: User = {
      email: decoded.sub,
      ...decoded
    };

    console.log('[AuthService] Loaded user from token:', user);
    this.userSubject.next(user);
  }

  whoAmI(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/whoami`);
  }
}