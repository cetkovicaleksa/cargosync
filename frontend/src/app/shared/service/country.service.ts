import { inject, Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { CountryOptionDto } from '../../factory/dto/country-option.dto';
import { CityOptionDto } from '../../factory/dto/city-option.dto';
import { HttpClient, HttpParams } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class CountryService {
  private readonly apiUrl = `${environment.apiHost}countries`;
  private readonly citiesApiUrl = `${environment.apiHost}cities`;
  private readonly http = inject(HttpClient);

  getCountries(): Observable<CountryOptionDto[]> {
    return this.http.get<CountryOptionDto[]>(`${this.apiUrl}`);
  }

  getCities(countryCode: string): Observable<CityOptionDto[]> {
    const params = new HttpParams().set('countryCode', countryCode);
    return this.http.get<CityOptionDto[]>(`${this.citiesApiUrl}`, { params });
  }
}
