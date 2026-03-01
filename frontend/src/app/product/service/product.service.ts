import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageDto } from '../../shared/dto/page.dto';
import { ProductDto } from '../dto/product.dto';
import { ProductSearchFilterDto } from '../dto/product-search-filter.dto';
import { SaveProductRequestDto } from '../dto/save-product-request.dto';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiHost}products`;

  search(filter: ProductSearchFilterDto, page: number, size: number, sort?: string): Observable<PageDto<ProductDto>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (sort && sort.trim().length) {
      params = params.set('sort', sort);
    }

    if (filter.name?.trim()) params = params.set('name', filter.name.trim());
    if (filter.categoryName?.trim()) params = params.set('categoryName', filter.categoryName.trim());
    if (filter.minPrice != null) params = params.set('minPrice', filter.minPrice);
    if (filter.maxPrice != null) params = params.set('maxPrice', filter.maxPrice);
    if (filter.isForSale != null) params = params.set('isForSale', filter.isForSale);

    return this.http.get<PageDto<ProductDto>>(this.apiUrl, { params });
  }

  create(request: SaveProductRequestDto, image?: File | null): Observable<ProductDto> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (image) {
      formData.append('image', image);
    }
    return this.http.post<ProductDto>(this.apiUrl, formData);
  }

  update(id: string, request: SaveProductRequestDto, image?: File | null): Observable<ProductDto> {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(request)], { type: 'application/json' }));
    if (image) {
      formData.append('image', image);
    }
    return this.http.put<ProductDto>(`${this.apiUrl}/${encodeURIComponent(id)}`, formData);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${encodeURIComponent(id)}`);
  }
}
