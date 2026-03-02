import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { Subject, Subscription, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';

import { FactoryService } from '../../service/factory.service';
import { FactoryAnalyticsService } from '../../service/factory-analytics.service';
import { FactoryDto } from '../../dto/factory.dto';
import { CityOptionDto } from '../../dto/city-option.dto';
import { CountryOptionDto } from '../../dto/country-option.dto';
import { ProductionSeriesDto } from '../../dto/production-series.dto';
import {
  FactoryAvailabilityRealtimeEventDto,
  FactoryAvailabilitySeriesDto,
} from '../../dto/factory-availability.dto';
import { ProtectedImageComponent } from '../../../shared/util/protected-image.component';
import { ImagePreviewDialogComponent } from '../../../shared/dialog/image-preview-dialog/image-preview-dialog.component';
import { CountryService } from '../../../shared/service/country.service';
import { FactoryLookupDto } from '../../dto/factory-lookup.dto';
import { ProductSummaryDto } from '../../../product/dto/product-summary.dto';

const ONE_DAY_MS = 24 * 60 * 60 * 1000;
const REALTIME_EVENT_SECONDS = 5;

const PLOT_WIDTH = 840;
const PLOT_HEIGHT = 260;
const PLOT_LEFT = 24;
const PLOT_RIGHT = PLOT_WIDTH - 16;
const PLOT_TOP = 12;
const PLOT_BOTTOM = PLOT_HEIGHT - 58;

type ProductionPeriodPreset = '7d' | '1m' | '3m' | '6m' | '1y' | 'custom';
type AvailabilityPeriodPreset = '1h' | '3h' | '12h' | '24h' | '7d' | '1m' | '3m' | '1y' | 'custom';

@Component({
  selector: 'app-factory-overview',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatTabsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    DecimalPipe,
    NgFor,
    NgIf,
    ProtectedImageComponent,
  ],
  templateUrl: './factory-overview.component.html',
  styleUrl: './factory-overview.component.scss',
})
export class FactoryOverviewComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly factories = inject(FactoryService);
  private readonly countryService = inject(CountryService);
  private readonly analytics = inject(FactoryAnalyticsService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  private readonly destroy$ = new Subject<void>();
  private realtimeSub?: Subscription;

  countries: CountryOptionDto[] = [];
  cities: CityOptionDto[] = [];
  factoriesData: FactoryLookupDto[] = [];

  products: ProductSummaryDto[] = [];
  productionSeries: ProductionSeriesDto | null = null;

  availabilitySeries: FactoryAvailabilitySeriesDto | null = null;
  realtimeActive = false;

  selectedFactory: FactoryDto | null = null;
  selectedProduct: ProductSummaryDto | null = null;

  loadingFactories = false;
  loadingProducts = false;
  loadingProduction = false;
  loadingAvailability = false;

  productionChartMax = 1;

  timeOptions = this.buildTimeOptions();

  factorySearchForm = this.fb.nonNullable.group({
    name: [''],
    countryCode: [''],
    cityCode: [''],
    sortBy: ['name'],
    sortDirection: ['asc' as 'asc' | 'desc'],
  });

  productionPeriodForm = this.fb.group({
    preset: this.fb.nonNullable.control<ProductionPeriodPreset>('7d'),
    fromDate: this.fb.control<Date | null>(null),
    toDate: this.fb.control<Date | null>(null),
  });

  availabilityPeriodForm = this.fb.group({
    preset: this.fb.nonNullable.control<AvailabilityPeriodPreset>('24h'),
    fromDate: this.fb.control<Date | null>(null),
    fromTime: this.fb.nonNullable.control('00:00'),
    toDate: this.fb.control<Date | null>(null),
    toTime: this.fb.nonNullable.control('23:59'),
  });

  ngOnInit(): void {
    this.countryService.getCountries().subscribe({
      next: (countries) => (this.countries = countries ?? []),
      error: () => this.snack.open('Failed to load countries.', 'Close', { duration: 3000 }),
    });

    this.factorySearchForm.controls.countryCode.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((countryCode) => {
        this.factorySearchForm.controls.cityCode.setValue('');
        this.cities = [];

        if (!countryCode) return;
        this.countryService.getCities(countryCode).subscribe({
          next: (cities) => (this.cities = cities ?? []),
          error: () => this.snack.open('Failed to load cities.', 'Close', { duration: 3000 }),
        });
      });

    this.factorySearchForm.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntil(this.destroy$),
      )
      .subscribe(() => this.loadFactories());

    this.productionPeriodForm.controls.preset.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((preset) => this.applyProductionPreset(preset));

    this.availabilityPeriodForm.controls.preset.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((preset) => {
        this.applyAvailabilityPreset(preset);
        this.stopRealtime();
      });

    this.applyProductionPreset('7d');
    this.applyAvailabilityPreset('24h');
    this.loadFactories();

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const factoryId = params.get('factoryId');
      if (!factoryId) return;
      this.selectFactory(factoryId);
    });
  }

  private buildTimeOptions(): string[] {
    const values: string[] = [];
    for (let hour = 0; hour < 24; hour += 1) {
      for (let minute = 0; minute < 60; minute += 30) {
        values.push(`${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`);
      }
    }
    return values;
  }

  private getFactoryFilter() {
    const value = this.factorySearchForm.getRawValue();
    return {
      name: value.name || null,
      countryCode: value.countryCode || null,
      cityCode: value.cityCode || null,
    };
  }

  private getSort() {
    const value = this.factorySearchForm.getRawValue();
    return {
      by: value.sortBy || 'name',
      direction: (value.sortDirection || 'asc') as 'asc' | 'desc',
    };
  }

  loadFactories() {
    if (this.loadingFactories) return;

    const sort = this.getSort();
    this.loadingFactories = true;

    this.factories
      .searchLookup(this.getFactoryFilter(), 0, 50, sort.by, sort.direction)
      .pipe(finalize(() => (this.loadingFactories = false)))
      .subscribe({
        next: (page) => {
          this.factoriesData = page.content ?? [];

          if (this.selectedFactory) {
            const refreshed = this.factoriesData.find((f) => f.id === this.selectedFactory?.id);
            if (refreshed) {
              this.selectFactory(refreshed.id);
            }
          }
        },
        error: () => this.snack.open('Failed to load factories.', 'Close', { duration: 3500 }),
      });
  }

  selectFactory(factoryId: string) {
    this.factories.getById(factoryId).subscribe({
      next: (factory) => {
        if (factory) {
          this.chooseFactory(factory);
        }
      },
    });
  }

  chooseFactory(factory: FactoryDto) {
    this.selectedFactory = factory;
    this.selectedProduct = null;
    this.products = [];
    this.productionSeries = null;
    this.availabilitySeries = null;
    this.stopRealtime();
    this.loadProductsForFactory(factory.id);
    this.loadFactoryAvailability();
  }

  openFactoryImage(imageUrl: string) {
    if (!imageUrl) return;
    this.dialog.open(ImagePreviewDialogComponent, {
      width: 'min(96vw, 1100px)',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: false,
      data: { imageUrl, title: 'Factory image' },
    });
  }

  private loadProductsForFactory(factoryId: string) {
    this.loadingProducts = true;
    this.analytics
      .getFactoryProducts(factoryId)
      .pipe(finalize(() => (this.loadingProducts = false)))
      .subscribe({
        next: (products) => {
          this.products = products ?? [];
          if (this.products.length === 0) {
            this.selectedProduct = null;
            this.productionSeries = null;
          }
        },
        error: () => this.snack.open('Failed to load factory products.', 'Close', { duration: 3500 }),
      });
  }

  chooseProduct(product: ProductSummaryDto) {
    this.selectedProduct = product;
    this.loadProductionSeries();
  }

  private applyProductionPreset(preset: ProductionPeriodPreset) {
    if (preset === 'custom') return;

    const to = new Date();
    const from = new Date(to.getTime());

    if (preset === '7d') from.setDate(from.getDate() - 6);
    if (preset === '1m') from.setMonth(from.getMonth() - 1);
    if (preset === '3m') from.setMonth(from.getMonth() - 3);
    if (preset === '6m') from.setMonth(from.getMonth() - 6);
    if (preset === '1y') from.setFullYear(from.getFullYear() - 1);

    from.setHours(0, 0, 0, 0);
    to.setHours(23, 59, 59, 999);

    this.productionPeriodForm.patchValue(
      {
        fromDate: from,
        toDate: to,
      },
      { emitEvent: false },
    );
  }

  private applyAvailabilityPreset(preset: AvailabilityPeriodPreset) {
    if (preset === 'custom') return;

    const to = new Date();
    const from = new Date(to.getTime());

    if (preset === '1h') from.setHours(from.getHours() - 1);
    if (preset === '3h') from.setHours(from.getHours() - 3);
    if (preset === '12h') from.setHours(from.getHours() - 12);
    if (preset === '24h') from.setHours(from.getHours() - 24);
    if (preset === '7d') from.setDate(from.getDate() - 7);
    if (preset === '1m') from.setMonth(from.getMonth() - 1);
    if (preset === '3m') from.setMonth(from.getMonth() - 3);
    if (preset === '1y') from.setFullYear(from.getFullYear() - 1);

    this.availabilityPeriodForm.patchValue(
      {
        fromDate: from,
        fromTime: this.toTimeString(from),
        toDate: to,
        toTime: this.toTimeString(to),
      },
      { emitEvent: false },
    );
  }

  private toTimeString(date: Date): string {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  }

  private toIsoDateOnly(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private combineDateAndTime(date: Date, hhmm: string): Date {
    const [hoursRaw, minutesRaw] = (hhmm || '00:00').split(':');
    const hours = Number(hoursRaw || 0);
    const minutes = Number(minutesRaw || 0);
    return new Date(date.getFullYear(), date.getMonth(), date.getDate(), hours, minutes, 0, 0);
  }

  private validatePeriod(from: Date, to: Date): boolean {
    if (!from || !to) {
      this.snack.open('Select both start and end dates.', 'Close', { duration: 3000 });
      return false;
    }

    if (from > to) {
      this.snack.open('Start date must be before end date.', 'Close', { duration: 3000 });
      return false;
    }

    const diff = Math.floor((to.getTime() - from.getTime()) / ONE_DAY_MS);
    if (diff > 365) {
      this.snack.open('Date range cannot be longer than one year.', 'Close', { duration: 3500 });
      return false;
    }

    return true;
  }

  private getProductionRange(): { from: string; to: string } | null {
    const value = this.productionPeriodForm.getRawValue();
    if (!value.fromDate || !value.toDate) {
      this.snack.open('Select both production dates.', 'Close', { duration: 3000 });
      return null;
    }

    const from = new Date(value.fromDate);
    from.setHours(0, 0, 0, 0);
    const to = new Date(value.toDate);
    to.setHours(23, 59, 59, 999);

    if (!this.validatePeriod(from, to)) return null;

    return {
      from: this.toIsoDateOnly(from),
      to: this.toIsoDateOnly(to),
    };
  }

  private getAvailabilityRange(): { from: string; to: string } | null {
    const value = this.availabilityPeriodForm.getRawValue();
    if (!value.fromDate || !value.toDate) {
      this.snack.open('Select availability date and time range.', 'Close', { duration: 3000 });
      return null;
    }

    const from = this.combineDateAndTime(value.fromDate, value.fromTime);
    const to = this.combineDateAndTime(value.toDate, value.toTime);

    if (!this.validatePeriod(from, to)) return null;

    return {
      from: from.toISOString(),
      to: to.toISOString(),
    };
  }

  loadProductionSeries() {
    if (!this.selectedFactory || !this.selectedProduct) return;

    const range = this.getProductionRange();
    if (!range) return;

    this.loadingProduction = true;
    this.analytics
      .getProductionSeries(this.selectedFactory.id, this.selectedProduct.id, range.from, range.to)
      .pipe(finalize(() => (this.loadingProduction = false)))
      .subscribe({
        next: (series) => {
          this.productionSeries = series;
          this.productionChartMax = Math.max(1, ...series.points.map((point) => point.units));
        },
        error: () => this.snack.open('Failed to load production series.', 'Close', { duration: 3500 }),
      });
  }

  loadFactoryAvailability() {
    if (!this.selectedFactory) return;

    const range = this.getAvailabilityRange();
    if (!range) return;

    this.loadingAvailability = true;
    this.analytics
      .getFactoryAvailability(this.selectedFactory.id, range.from, range.to)
      .pipe(finalize(() => (this.loadingAvailability = false)))
      .subscribe({
        next: (series) => {
          this.availabilitySeries = series;
        },
        error: () => this.snack.open('Failed to load factory availability.', 'Close', { duration: 3500 }),
      });
  }

  get canUseRealtime(): boolean {
    const range = this.getAvailabilityRange();
    if (!range) return false;

    const from = new Date(range.from);
    const to = new Date(range.to);
    const diffHours = (to.getTime() - from.getTime()) / (60 * 60 * 1000);
    const untilNowMs = Date.now() - to.getTime();

    return diffHours > 0 && diffHours <= 3.05 && untilNowMs >= -30_000 && untilNowMs <= 10 * 60_000;
  }

  toggleRealtime() {
    if (this.realtimeActive) {
      this.stopRealtime();
      return;
    }

    if (!this.selectedFactory || !this.canUseRealtime) {
      this.snack.open('Realtime is available only for the last 3 hours window.', 'Close', { duration: 3200 });
      return;
    }

    if (!this.availabilitySeries) {
      this.loadFactoryAvailability();
    }

    this.realtimeActive = true;
    this.realtimeSub = this.analytics
      .subscribeFactoryAvailabilityRealtime(this.selectedFactory.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event) => this.applyRealtimeEvent(event),
        error: () => {
          this.stopRealtime();
          this.snack.open('Realtime stream failed.', 'Close', { duration: 3000 });
        },
      });
  }

  private stopRealtime() {
    this.realtimeActive = false;
    this.realtimeSub?.unsubscribe();
    this.realtimeSub = undefined;
  }

  private applyRealtimeEvent(event: FactoryAvailabilityRealtimeEventDto) {
    if (!this.availabilitySeries) return;

    const eventDate = new Date(event.timestamp);
    const bucketStart = new Date(eventDate);
    bucketStart.setMinutes(0, 0, 0);
    const bucketEnd = new Date(bucketStart);
    bucketEnd.setHours(bucketEnd.getHours() + 1);

    const key = bucketStart.toISOString();
    let point = this.availabilitySeries.points.find((entry) => entry.periodStart === key);

    if (!point) {
      point = {
        periodStart: key,
        periodEnd: bucketEnd.toISOString(),
        onlineSeconds: 0,
        offlineSeconds: 0,
        onlinePercent: 0,
        offlinePercent: 0,
      };
      this.availabilitySeries.points = [...this.availabilitySeries.points, point].sort(
        (a, b) => new Date(a.periodStart).getTime() - new Date(b.periodStart).getTime(),
      );
    }

    if (event.online) {
      point.onlineSeconds += REALTIME_EVENT_SECONDS;
    } else {
      point.offlineSeconds += REALTIME_EVENT_SECONDS;
    }

    const total = Math.max(1, point.onlineSeconds + point.offlineSeconds);
    point.onlinePercent = Number(((point.onlineSeconds * 100) / total).toFixed(2));
    point.offlinePercent = Number((100 - point.onlinePercent).toFixed(2));

    this.recalculateAvailabilityTotals();
  }

  private recalculateAvailabilityTotals() {
    if (!this.availabilitySeries) return;

    const totalOnline = this.availabilitySeries.points.reduce((sum, point) => sum + point.onlineSeconds, 0);
    const totalOffline = this.availabilitySeries.points.reduce((sum, point) => sum + point.offlineSeconds, 0);
    const total = Math.max(1, totalOnline + totalOffline);

    this.availabilitySeries.totalOnlineSeconds = totalOnline;
    this.availabilitySeries.totalOfflineSeconds = totalOffline;
    this.availabilitySeries.onlinePercent = Number(((totalOnline * 100) / total).toFixed(2));
    this.availabilitySeries.offlinePercent = Number((100 - this.availabilitySeries.onlinePercent).toFixed(2));
  }

  productionBarHeight(units: number): number {
    return Math.max(6, Math.round((units / this.productionChartMax) * 180));
  }

  productionPointLabel(index: number): string {
    if (!this.productionSeries) return '';
    const point = this.productionSeries.points[index];
    if (!point) return '';

    if (this.productionSeries.bucket === 'day') {
      return point.periodStart;
    }

    return `${point.periodStart} → ${point.periodEnd}`;
  }

  productionTickIndices(): number[] {
    const series = this.productionSeries;
    if (!series || series.points.length === 0) return [];

    const count = series.points.length;
    const maxTicks = 10;

    if (count <= maxTicks) {
      return Array.from({ length: count }, (_, i) => i);
    }

    const step = Math.ceil(count / maxTicks);
    const indices = new Set<number>();
    for (let i = 0; i < count; i += step) {
      indices.add(i);
    }
    indices.add(count - 1);

    return Array.from(indices).sort((a, b) => a - b);
  }

  productionTickLabel(index: number): string {
    const series = this.productionSeries;
    if (!series || !series.points[index]) return '';

    const point = series.points[index];
    const date = new Date(point.periodStart);

    if (series.bucket === 'day') {
      return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }

    if (series.bucket === 'week') {
      return `W ${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  availabilityPieGradient(): string {
    if (!this.availabilitySeries) return 'conic-gradient(#4caf50 0%, #f44336 0%)';
    const online = Math.max(0, Math.min(100, this.availabilitySeries.onlinePercent));
    return `conic-gradient(#2e7d32 ${online}%, #d32f2f ${online}% 100%)`;
  }

  availabilityLinePoints(): string {
    const series = this.availabilitySeries;
    if (!series || series.points.length === 0) return '';

    const step = series.points.length > 1 ? (PLOT_RIGHT - PLOT_LEFT) / (series.points.length - 1) : 0;

    return series.points
      .map((point, index) => {
        const x = PLOT_LEFT + step * index;
        const y = PLOT_BOTTOM - (point.onlinePercent / 100) * (PLOT_BOTTOM - PLOT_TOP);
        return `${x},${y}`;
      })
      .join(' ');
  }

  availabilityTickIndices(): number[] {
    const series = this.availabilitySeries;
    if (!series || series.points.length === 0) return [];

    const count = series.points.length;
    const maxTicks = 8;

    if (count <= maxTicks) {
      return Array.from({ length: count }, (_, i) => i);
    }

    const step = Math.ceil(count / maxTicks);
    const indices = new Set<number>();
    for (let i = 0; i < count; i += step) {
      indices.add(i);
    }
    indices.add(count - 1);

    return Array.from(indices).sort((a, b) => a - b);
  }

  availabilityTickX(index: number): number {
    const series = this.availabilitySeries;
    if (!series || series.points.length <= 1) return PLOT_LEFT;
    const step = (PLOT_RIGHT - PLOT_LEFT) / (series.points.length - 1);
    return PLOT_LEFT + step * index;
  }

  availabilityTickLabel(index: number): string {
    const series = this.availabilitySeries;
    if (!series || !series.points[index]) return '';

    const date = new Date(series.points[index].periodStart);

    if (series.bucket === 'hour') {
      return `${String(date.getHours()).padStart(2, '0')}:00`;
    }

    if (series.bucket === 'day') {
      return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }

    if (series.bucket === 'week') {
      return `W ${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  availabilityTickTransform(index: number): string {
    const x = this.availabilityTickX(index);
    return `translate(${x}, ${PLOT_BOTTOM + 24}) rotate(-35)`;
  }

  availabilityLabel(iso: string): string {
    const date = new Date(iso);
    const series = this.availabilitySeries;
    if (!series) return date.toISOString();

    if (series.bucket === 'hour') {
      return `${String(date.getHours()).padStart(2, '0')}:00`;
    }

    if (series.bucket === 'day') {
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }

    if (series.bucket === 'week') {
      return `W ${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    }

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  formatDuration(seconds: number): string {
    const s = Math.max(0, Math.floor(seconds));
    const days = Math.floor(s / 86400);
    const hours = Math.floor((s % 86400) / 3600);
    const minutes = Math.floor((s % 3600) / 60);

    if (days > 0) return `${days}d ${hours}h ${minutes}m`;
    return `${hours}h ${minutes}m`;
  }

  ngOnDestroy(): void {
    this.stopRealtime();
    this.destroy$.next();
    this.destroy$.complete();
  }
}
