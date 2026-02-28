import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompanyDto } from '../dto/company.dto';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);

  getMyCompanies(): Observable<CompanyDto[]> {
    return this.http.get<CompanyDto[]>('/api/companies/mine');
  }
}
