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

  getByIds(ids: string[]): Observable<FactoryDto[]> {
    let params = new HttpParams();
    ids.forEach((id) => {
      params = params.append('ids', id);
    });
    return this.http.get<FactoryDto[]>(`${this.apiUrl}/by-ids`, { params });
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

  getCountries(): Observable<CountryOptionDto[]> {
    return this.http.get<CountryOptionDto[]>(`${this.apiUrl}/countries`);
  }

  getCities(countryCode: string): Observable<CityOptionDto[]> {
    const params = new HttpParams().set('countryCode', countryCode);
    return this.http.get<CityOptionDto[]>(`${this.apiUrl}/cities`, { params });
  }
}
