import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageDto } from '../../shared/dto/page.dto';
import { WarehouseDto } from '../dto/warehouse.dto';

export interface WarehouseFilterParams {
  name?: string | null;
  countryCode?: string | null;
  cityCode?: string | null;
  online?: boolean | null;
}

@Injectable({ providedIn: 'root' })
export class WarehouseService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}warehouses`;

  list(filter: WarehouseFilterParams, page: number, size: number): Observable<PageDto<WarehouseDto>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (filter.name?.trim()) params = params.set('name', filter.name.trim());
    if (filter.countryCode?.trim()) params = params.set('countryCode', filter.countryCode.trim());
    if (filter.cityCode?.trim()) params = params.set('cityCode', filter.cityCode.trim());
    if (filter.online != null) params = params.set('online', filter.online);

    return this.http.get<PageDto<WarehouseDto>>(this.apiUrl, { params });
  }

  getById(id: string): Observable<WarehouseDto> {
    return this.http.get<WarehouseDto>(`${this.apiUrl}/${id}`);
  }

  create(formData: FormData): Observable<WarehouseDto> {
    return this.http.post<WarehouseDto>(this.apiUrl, formData);
  }

  update(id: string, formData: FormData): Observable<WarehouseDto> {
    return this.http.put<WarehouseDto>(`${this.apiUrl}/${id}`, formData);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
