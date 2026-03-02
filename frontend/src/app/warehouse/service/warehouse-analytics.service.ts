import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TemperatureChartDto, TemperatureChartPeriod } from '../dto/temperature-chart.dto';

@Injectable({ providedIn: 'root' })
export class WarehouseAnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiHost}warehouses`;

  getTemperatureChart(
    warehouseId: string,
    sectorId: string,
    period: TemperatureChartPeriod,
    from?: string,
    to?: string,
  ): Observable<TemperatureChartDto> {
    let params = new HttpParams().set('period', period);
    if (period === 'CUSTOM' && from && to) {
      params = params.set('from', from).set('to', to);
    }
    return this.http.get<TemperatureChartDto>(
      `${this.apiUrl}/${warehouseId}/sectors/${sectorId}/temperature-chart`,
      { params },
    );
  }
}
