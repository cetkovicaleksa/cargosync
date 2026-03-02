import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, defer, interval, map, Observable, of } from 'rxjs';
import { webSocket } from 'rxjs/webSocket';
import { environment } from '../../../environments/environment';
import { ProductionBucket, ProductionSeriesDto } from '../dto/production-series.dto';
import {
  AvailabilityBucket,
  FactoryAvailabilityPointDto,
  FactoryAvailabilityRealtimeEventDto,
  FactoryAvailabilitySeriesDto,
} from '../dto/factory-availability.dto';
import { ProductSummaryDto } from '../../product/dto/product-summary.dto';

@Injectable({ providedIn: 'root' })
export class FactoryAnalyticsService {
  private readonly http = inject(HttpClient);

  private readonly apiBase = environment.apiHost;

  getFactoryProducts(factoryId: string): Observable<ProductSummaryDto[]> {
    const url = `${this.apiBase}factories/${encodeURIComponent(factoryId)}/products`;

    return this.http.get<ProductSummaryDto[]>(url).pipe(
      // catchError(() => of(this.mockProducts(factoryId))),
    );
  }

  getProductionSeries(factoryId: string, productId: string, from: string, to: string): Observable<ProductionSeriesDto> {
    const url = `${this.apiBase}factories/${encodeURIComponent(factoryId)}/products/${encodeURIComponent(productId)}/production`;
    const params = new HttpParams().set('from', from).set('to', to);

    return this.http.get<ProductionSeriesDto>(url, { params }).pipe(
      catchError(() => of(this.mockSeries(factoryId, productId, from, to))),
    );
  }

  getFactoryAvailability(factoryId: string, from: string, to: string): Observable<FactoryAvailabilitySeriesDto> {
    const url = `${this.apiBase}factories/${encodeURIComponent(factoryId)}/availability`;
    const params = new HttpParams().set('from', from).set('to', to);

    return this.http.get<FactoryAvailabilitySeriesDto>(url, { params }).pipe(
      catchError(() => of(this.mockAvailability(factoryId, from, to))),
    );
  }

  subscribeFactoryAvailabilityRealtime(factoryId: string): Observable<FactoryAvailabilityRealtimeEventDto> {
    const wsUrl = this.toWsUrl(`${this.apiBase}factories/${encodeURIComponent(factoryId)}/availability/ws`);

    return defer(() => webSocket<FactoryAvailabilityRealtimeEventDto>(wsUrl)).pipe(
      map((message) => ({
        timestamp: message?.timestamp || new Date().toISOString(),
        online: !!message?.online,
      })),
      catchError(() => this.mockRealtimeEvents(factoryId)),
    );
  }

  private mockProducts(factoryId: string): ProductSummaryDto[] {
    const seed = this.hash(factoryId);
    const names = ['Steel Beam', 'Copper Wire', 'Aluminum Plate', 'Industrial Valve', 'Hydraulic Pump', 'Smart Sensor'];

    return names.map((name, index) => ({
      id: this.makeStableId(factoryId, index),
      name: `${name} ${((seed + index) % 7) + 1}`,
      categoryName: ['Metals', 'Electronics', 'Machinery'][index % 3],
      imageUrl: null,
    }));
  }

  private mockSeries(factoryId: string, productId: string, fromIso: string, toIso: string): ProductionSeriesDto {
    const from = new Date(fromIso);
    const to = new Date(toIso);

    const diffDays = Math.max(1, Math.floor((to.getTime() - from.getTime()) / (24 * 60 * 60 * 1000)) + 1);
    const bucket: ProductionBucket = diffDays <= 45 ? 'day' : diffDays <= 180 ? 'week' : 'month';

    const points = this.generatePoints(factoryId, productId, from, to, bucket);
    const totalUnits = points.reduce((sum, point) => sum + point.units, 0);

    return {
      factoryId,
      productId,
      bucket,
      totalUnits,
      points,
    };
  }

  private generatePoints(factoryId: string, productId: string, from: Date, to: Date, bucket: ProductionBucket) {
    const points: Array<{ periodStart: string; periodEnd: string; units: number }> = [];
    const seed = this.hash(`${factoryId}|${productId}`);

    let cursor = this.startOfDay(from);

    while (cursor <= to) {
      let next: Date;
      if (bucket === 'day') {
        next = this.addDays(cursor, 1);
      } else if (bucket === 'week') {
        next = this.addDays(cursor, 7);
      } else {
        next = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1);
      }

      const periodEnd = new Date(Math.min(this.addDays(next, -1).getTime(), to.getTime()));
      const index = points.length;
      const units = this.mockUnits(seed, index, bucket);

      points.push({
        periodStart: this.toIsoDate(cursor),
        periodEnd: this.toIsoDate(periodEnd),
        units,
      });

      cursor = next;
    }

    return points;
  }

  private mockUnits(seed: number, index: number, bucket: ProductionBucket): number {
    const wave = Math.sin((seed % 17 + index) * 0.8) * 0.35 + 0.65;
    const base = bucket === 'day' ? 80 : bucket === 'week' ? 520 : 2100;
    const variance = (seed % 23) * 7 + (index % 5) * 13;
    return Math.max(0, Math.round(base * wave + variance));
  }

  private mockAvailability(factoryId: string, fromIso: string, toIso: string): FactoryAvailabilitySeriesDto {
    const from = new Date(fromIso);
    const to = new Date(toIso);
    const diffMs = Math.max(ONE_MINUTE_MS, to.getTime() - from.getTime());
    const diffHours = diffMs / (60 * 60 * 1000);
    const diffDays = diffHours / 24;

    const bucket: AvailabilityBucket =
      diffHours <= 48 ? 'hour' :
      diffDays <= 45 ? 'day' :
      diffDays <= 180 ? 'week' : 'month';

    const points = this.generateAvailabilityPoints(factoryId, from, to, bucket);
    const totalOnlineSeconds = points.reduce((sum, point) => sum + point.onlineSeconds, 0);
    const totalOfflineSeconds = points.reduce((sum, point) => sum + point.offlineSeconds, 0);
    const total = Math.max(1, totalOnlineSeconds + totalOfflineSeconds);

    return {
      factoryId,
      bucket,
      totalOnlineSeconds,
      totalOfflineSeconds,
      onlinePercent: Number(((totalOnlineSeconds * 100) / total).toFixed(2)),
      offlinePercent: Number(((totalOfflineSeconds * 100) / total).toFixed(2)),
      points,
    };
  }

  private generateAvailabilityPoints(
    factoryId: string,
    from: Date,
    to: Date,
    bucket: AvailabilityBucket,
  ): FactoryAvailabilityPointDto[] {
    const seed = this.hash(factoryId);
    const points: FactoryAvailabilityPointDto[] = [];

    let cursor = this.startOfBucket(from, bucket);
    while (cursor <= to) {
      const next = this.nextBucket(cursor, bucket);
      const end = new Date(Math.min(next.getTime(), to.getTime()));

      const durationSeconds = Math.max(60, Math.round((end.getTime() - cursor.getTime()) / 1000));
      const index = points.length;
      const onlineRatio = this.clamp(0.35, 0.99, 0.78 + Math.sin((seed % 31 + index) * 0.62) * 0.16);
      const onlineSeconds = Math.round(durationSeconds * onlineRatio);
      const offlineSeconds = Math.max(0, durationSeconds - onlineSeconds);

      points.push({
        periodStart: cursor.toISOString(),
        periodEnd: end.toISOString(),
        onlineSeconds,
        offlineSeconds,
        onlinePercent: Number(((onlineSeconds * 100) / durationSeconds).toFixed(2)),
        offlinePercent: Number(((offlineSeconds * 100) / durationSeconds).toFixed(2)),
      });

      cursor = next;
    }

    return points;
  }

  private mockRealtimeEvents(factoryId: string): Observable<FactoryAvailabilityRealtimeEventDto> {
    const seed = this.hash(`${factoryId}:realtime`);
    return interval(5000).pipe(
      map((index) => ({
        timestamp: new Date().toISOString(),
        online: Math.sin((seed + index) * 0.75) > -0.15,
      })),
    );
  }

  private toWsUrl(url: string): string {
    if (url.startsWith('https://')) return `wss://${url.slice('https://'.length)}`;
    if (url.startsWith('http://')) return `ws://${url.slice('http://'.length)}`;
    return url;
  }

  private startOfBucket(date: Date, bucket: AvailabilityBucket): Date {
    const d = new Date(date);
    if (bucket === 'hour') {
      d.setMinutes(0, 0, 0);
      return d;
    }
    if (bucket === 'day') {
      d.setHours(0, 0, 0, 0);
      return d;
    }
    if (bucket === 'week') {
      d.setHours(0, 0, 0, 0);
      const day = d.getDay() || 7;
      d.setDate(d.getDate() - day + 1);
      return d;
    }
    d.setHours(0, 0, 0, 0);
    d.setDate(1);
    return d;
  }

  private nextBucket(date: Date, bucket: AvailabilityBucket): Date {
    const d = new Date(date);
    if (bucket === 'hour') {
      d.setHours(d.getHours() + 1);
      return d;
    }
    if (bucket === 'day') {
      d.setDate(d.getDate() + 1);
      return d;
    }
    if (bucket === 'week') {
      d.setDate(d.getDate() + 7);
      return d;
    }
    d.setMonth(d.getMonth() + 1);
    return d;
  }

  private clamp(min: number, max: number, value: number): number {
    return Math.max(min, Math.min(max, value));
  }

  private hash(value: string): number {
    let h = 0;
    for (let i = 0; i < value.length; i += 1) {
      h = (h * 31 + value.charCodeAt(i)) | 0;
    }
    return Math.abs(h);
  }

  private makeStableId(factoryId: string, index: number): string {
    const hex = (this.hash(`${factoryId}:${index}`).toString(16)).padStart(32, '0').slice(0, 32);
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20, 32)}`;
  }

  private startOfDay(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  private addDays(date: Date, days: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
  }

  private toIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}

const ONE_MINUTE_MS = 60 * 1000;
