import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageDto } from '../../shared/dto/page.dto';
import { FactoryDto } from '../dto/factory.dto';
import { FactorySearchFilterDto } from '../dto/factory-search-filter.dto';
import { SaveFactoryRequestDto } from '../dto/save-factory-request.dto';
import { CountryOptionDto } from '../dto/country-option.dto';
import { CityOptionDto } from '../dto/city-option.dto';
import { FactoryLookupDto } from '../dto/factory-lookup.dto';

@Injectable({ providedIn: 'root' })
export class FactoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}factories`;

  search(
    filter: FactorySearchFilterDto,
    page: number,
    size: number,
    sortBy?: string,
    direction?: 'asc' | 'desc',
  ): Observable<PageDto<FactoryDto>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (sortBy?.trim()) {
      params = params.set('sort', `${sortBy.trim()},${direction ?? 'asc'}`);
    }

    if (filter.name?.trim()) params = params.set('name', filter.name.trim());
    if (filter.countryCode?.trim()) params = params.set('countryCode', filter.countryCode.trim());
    if (filter.cityCode?.trim()) params = params.set('cityCode', filter.cityCode.trim());

    return this.http.get<PageDto<FactoryDto>>(this.apiUrl, { params });
  }
  
  searchLookup(
    filter: FactorySearchFilterDto,
    page: number,
    size: number,
    sortBy?: string,
    direction?: 'asc' | 'desc',
  ): Observable<PageDto<FactoryLookupDto>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (sortBy?.trim()) {
      params = params.set('sort', `${sortBy.trim()},${direction ?? 'asc'}`);
    }

    if (filter.name?.trim()) params = params.set('name', filter.name.trim());
    if (filter.countryCode?.trim()) params = params.set('countryCode', filter.countryCode.trim());
    if (filter.cityCode?.trim()) params = params.set('cityCode', filter.cityCode.trim());

    return this.http.get<PageDto<FactoryLookupDto>>(`${this.apiUrl}/lookup`, { params });
  }

  getByIds(ids: string[]): Observable<FactoryDto[]> {
    let params = new HttpParams();
    ids.forEach((id) => {
      params = params.append('ids', id);
    });
    return this.http.get<FactoryDto[]>(`${this.apiUrl}/by-ids`, { params });
  }
  
  getByIdsLookup(ids: string[]): Observable<FactoryLookupDto[]> {
    let params = new HttpParams();
    ids.forEach((id) => {
      params = params.append('ids', id);
    });
    return this.http.get<FactoryLookupDto[]>(`${this.apiUrl}/by-ids/lookup`, { params });
  }

  getById(id: string): Observable<FactoryDto | null> {
    return this.http.get<FactoryDto>(`${this.apiUrl}/${encodeURIComponent(id)}`);
  }

  create(request: SaveFactoryRequestDto, images: File[] = []): Observable<FactoryDto> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    images.forEach((image) => formData.append('images', image));
    return this.http.post<FactoryDto>(this.apiUrl, formData);
  }

  update(id: string, request: SaveFactoryRequestDto, images: File[] = []): Observable<FactoryDto> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    images.forEach((image) => formData.append('images', image));
    return this.http.put<FactoryDto>(`${this.apiUrl}/${encodeURIComponent(id)}`, formData);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(id)}`);
  }
}
