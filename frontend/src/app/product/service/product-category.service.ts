import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProductCategoryService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}categories`;

  getAllCategoryNames(): Observable<string[]> {
    return this.http.get<string[]>(this.apiUrl);
  }
}
