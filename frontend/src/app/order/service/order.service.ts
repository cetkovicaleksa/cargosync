import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageDto } from '../../shared/dto/page.dto';
import { OrderDto } from '../dto/order.dto';
import { CreateOrderRequestDto } from '../dto/create-order-request.dto';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}orders`;

  create(request: CreateOrderRequestDto): Observable<OrderDto> {
    return this.http.post<OrderDto>(this.apiUrl, request);
  }

  getMine(page: number, size: number): Observable<PageDto<OrderDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageDto<OrderDto>>(`${this.apiUrl}/mine`, { params });
  }

  getById(id: string): Observable<OrderDto> {
    return this.http.get<OrderDto>(`${this.apiUrl}/${encodeURIComponent(id)}`);
  }
}
