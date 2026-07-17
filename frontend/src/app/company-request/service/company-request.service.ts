import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageDto } from '../../shared/dto/page.dto';
import { CompanyRequestDto, CompanyRequestStatus } from '../dto/company-request.dto';
import { CreateCompanyRequestDto } from '../dto/create-company-request.dto';
import { RejectCompanyRequestDto } from '../dto/reject-company-request.dto';

@Injectable({ providedIn: 'root' })
export class CompanyRequestService {
  private readonly http = inject(HttpClient);
  private readonly companyApiUrl = `${environment.apiHost}companies`;
  private readonly companyRequestApiUrl = `${environment.apiHost}company-requests`;

  create(payload: CreateCompanyRequestDto, images: File[], proofs: File[]): Observable<void> {
    const formData = new FormData();
    formData.append('name', payload.name);
    formData.append('address', payload.address);
    formData.append('city.countryCode', payload.city.countryCode);
    formData.append('city.code', payload.city.code);
    formData.append('location.latitude', String(payload.location.latitude));
    formData.append('location.longitude', String(payload.location.longitude));

    images.forEach((image) => formData.append('images', image));
    proofs.forEach((proof) => formData.append('proofs', proof));

    return this.http.post<void>(this.companyApiUrl, formData);
  }

  list(status: CompanyRequestStatus, page: number, size: number): Observable<PageDto<CompanyRequestDto>> {
    const params = new HttpParams()
      .set('status', status)
      .set('page', page)
      .set('size', size);

    return this.http.get<PageDto<CompanyRequestDto>>(this.companyRequestApiUrl, { params });
  }

  getById(id: string): Observable<CompanyRequestDto> {
    return this.http.get<CompanyRequestDto>(`${this.companyRequestApiUrl}/${encodeURIComponent(id)}`);
  }

  getByIdWithEtag(id: string): Observable<{ request: CompanyRequestDto; etag: string }> {
    return this.http
      .get<CompanyRequestDto>(`${this.companyRequestApiUrl}/${encodeURIComponent(id)}`, {
        observe: 'response',
      })
      .pipe(
        map((response: HttpResponse<CompanyRequestDto>) => {
          const etag = response.headers.get('ETag');
          if (!etag) {
            throw new Error('Missing ETag response header for company request.');
          }

          return {
            request: response.body as CompanyRequestDto,
            etag,
          };
        }),
      );
  }

  approve(id: string, etag: string): Observable<void> {
    const headers = new HttpHeaders({ 'If-Match': etag });
    return this.http.post<void>(`${this.companyRequestApiUrl}/${encodeURIComponent(id)}/approve`, null, { headers });
  }

  reject(id: string, etag: string, body: RejectCompanyRequestDto): Observable<void> {
    const headers = new HttpHeaders({ 'If-Match': etag });
    return this.http.post<void>(`${this.companyRequestApiUrl}/${encodeURIComponent(id)}/reject`, body, { headers });
  }

  getAttachmentBlob(path: string): Observable<Blob> {
    const normalized = path.startsWith('/') ? path.slice(1) : path;
    const url = path.startsWith('http://') || path.startsWith('https://')
      ? path
      : `${environment.imageBaseUrl}${normalized}`;

    return this.http.get(url, { responseType: 'blob' });
  }
}
