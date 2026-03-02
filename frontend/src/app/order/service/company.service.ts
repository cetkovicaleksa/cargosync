import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CompanyDto } from '../dto/company.dto';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}companies`;

  getMyCompanies(): Observable<CompanyDto[]> {
    return this.http.get<CompanyDto[]>(`${this.apiUrl}/mine`);
  }
}
