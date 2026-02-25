import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { PageDto } from '../../shared/dto/page.dto';
import { AccountDto } from '../../auth/dto/account.dto';

@Injectable({ providedIn: 'root' })
export class ManagerService {
  private readonly http = inject(HttpClient);

  // Backend: @RequestMapping("/api/managers")
  private readonly apiUrl = `${environment.apiHost}managers`;

  getManagers(query: string | null, page: number, size = 25): Observable<PageDto<AccountDto>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (query && query.trim().length) {
      params = params.set('query', query.trim());
    }

    return this.http.get<PageDto<AccountDto>>(this.apiUrl, { params });
  }

  toggleBlock(username: string): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${encodeURIComponent(username)}/toggle-block`, {});
  }

  createManager(payload: { email: string; username: string; avatarUrl: string }): Observable<AccountDto> {
    return this.http.post<AccountDto>(this.apiUrl, payload);
  }
}
