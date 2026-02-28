import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageDto } from '../../shared/dto/page.dto';
import { FactoryDto } from '../dto/factory.dto';
import { FactorySearchFilterDto } from '../dto/factory-search-filter.dto';

@Injectable({ providedIn: 'root' })
export class FactoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}factories`;

  search(filter: FactorySearchFilterDto, page: number, size: number): Observable<PageDto<FactoryDto>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (filter.name?.trim()) params = params.set('name', filter.name.trim());
    if (filter.countryCode?.trim()) params = params.set('countryCode', filter.countryCode.trim());
    if (filter.cityCode?.trim()) params = params.set('cityCode', filter.cityCode.trim());

    return this.http.get<PageDto<FactoryDto>>(this.apiUrl, { params });
  }

  getByIds(ids: string[]): Observable<FactoryDto[]> {
    let params = new HttpParams();
    ids.forEach((id) => {
      params = params.append('ids', id);
    });
    return this.http.get<FactoryDto[]>(`${this.apiUrl}/by-ids`, { params });
  }
}
