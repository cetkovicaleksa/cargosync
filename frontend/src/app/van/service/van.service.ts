import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageDto } from '../../shared/dto/page.dto';
import { VanDto } from '../dto/van.dto';
import { VanSearchFilterDto } from '../dto/van-search-filter.dto';
import { SaveVanRequestDto } from '../dto/save-van-request.dto';
import { MakeDto } from '../dto/make.dto';
import { VanStatusDto } from '../dto/van-status.dto';
import { VanDistanceSeriesDto } from '../dto/van-distance-series.dto';

@Injectable({ providedIn: 'root' })
export class VanService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}vans`;
  private readonly makesUrl = `${environment.apiHost}vans/makes`;

  search(
    filter: VanSearchFilterDto,
    page: number,
    size: number,
    sortBy?: string,
    direction?: 'asc' | 'desc',
  ): Observable<PageDto<VanDto>> {
    let params = new HttpParams().set('page', page).set('size', size);

    if (sortBy?.trim()) {
      params = params.set('sort', `${sortBy.trim()},${direction ?? 'asc'}`);
    }

    if (filter.q?.trim()) params = params.set('q', filter.q.trim());
    if (filter.make?.trim()) params = params.set('make', filter.make.trim());
    if (filter.model?.trim()) params = params.set('model', filter.model.trim());
    if (filter.canCarryKg != null) params = params.set('canCarryKg', filter.canCarryKg);
    if (filter.tooBigIfCanCarryKg != null) params = params.set('tooBigIfCanCarryKg', filter.tooBigIfCanCarryKg);

    return this.http.get<PageDto<VanDto>>(this.apiUrl, { params });
  }

  getByVin(vin: string): Observable<VanDto> {
    return this.http.get<VanDto>(`${this.apiUrl}/${encodeURIComponent(vin)}`);
  }

  getMakes(): Observable<MakeDto[]> {
    return this.http.get<MakeDto[]>(this.makesUrl);
  }

  create(request: SaveVanRequestDto, images: File[]): Observable<VanDto> {
    const formData = new FormData();
    formData.append('registrationNumber', request.registrationNumber);
    formData.append('maxPayload', String(request.maxPayload));
    formData.append('model.wmi', request.model.wmi);
    formData.append('model.vd', request.model.vd);
    images.forEach((image) => formData.append('images', image));

    return this.http.post<VanDto>(this.apiUrl, formData);
  }

  update(
    vin: string,
    request: SaveVanRequestDto,
    newImages: File[],
    removeImages: string[],
  ): Observable<VanDto> {
    const formData = new FormData();
    formData.append('registrationNumber', request.registrationNumber);
    formData.append('maxPayload', String(request.maxPayload));
    formData.append('model.wmi', request.model.wmi);
    formData.append('model.vd', request.model.vd);
    formData.append('version', String(request.version ?? ''));
    newImages.forEach((image) => formData.append('newImages', image));
    removeImages.forEach((image) => formData.append('removeImages', image));

    const headers = new HttpHeaders({ 'If-Match': this.serializeEtag(request.version) });
    return this.http.patch<VanDto>(`${this.apiUrl}/${encodeURIComponent(vin)}`, formData, { headers });
  }

  delete(vin: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(vin)}`);
  }

  getStatus(vin: string): Observable<VanStatusDto> {
    return this.http.get<VanStatusDto>(`${this.apiUrl}/${encodeURIComponent(vin)}/status`);
  }

  getDistanceSeries(vin: string, from: string, to: string): Observable<VanDistanceSeriesDto> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<VanDistanceSeriesDto>(`${this.apiUrl}/${encodeURIComponent(vin)}/distance`, { params });
  }

  private serializeEtag(version: number | null | undefined): string {
    if (version == null || Number.isNaN(Number(version))) {
      return '""';
    }

    return `"${version}"`;
  }
}
