import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DatePipe, DecimalPipe, NgFor, NgIf } from '@angular/common';
import { Subject, finalize, takeUntil } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { WarehouseService } from '../../service/warehouse.service';
import { WarehouseAnalyticsService } from '../../service/warehouse-analytics.service';
import { WarehouseDto, WarehouseSectorDto } from '../../dto/warehouse.dto';
import { TemperatureChartDto, TemperatureChartPeriod, TemperatureDataPointDto } from '../../dto/temperature-chart.dto';
import { ProtectedImageComponent } from '../../../shared/util/protected-image.component';
import {
  ImagePreviewDialogComponent,
  ImagePreviewDialogData,
} from '../../../shared/dialog/image-preview-dialog/image-preview-dialog.component';

const ONE_DAY_MS = 24 * 60 * 60 * 1000;

const PLOT_LEFT   = 48;
const PLOT_RIGHT  = 824;
const PLOT_TOP    = 12;
const PLOT_BOTTOM = 202;

type PeriodPreset = TemperatureChartPeriod;
interface ChartStats { min: number; max: number; avg: number; }

@Component({
  selector: 'app-warehouse-overview',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatDialogModule,
    DatePipe,
    DecimalPipe,
    NgFor,
    NgIf,
    ProtectedImageComponent,
  ],
  templateUrl: './warehouse-overview.component.html',
  styleUrl:    './warehouse-overview.component.scss',
})
export class WarehouseOverviewComponent implements OnInit, OnDestroy {
  private readonly fb               = inject(FormBuilder);
  private readonly route            = inject(ActivatedRoute);
  private readonly warehouseService = inject(WarehouseService);
  private readonly analytics        = inject(WarehouseAnalyticsService);
  private readonly snack            = inject(MatSnackBar);
  private readonly dialog           = inject(MatDialog);
  private readonly destroy$         = new Subject<void>();

  warehouse:      WarehouseDto       | null = null;
  selectedSector: WarehouseSectorDto | null = null;
  chartData:      TemperatureChartDto | null = null;
  chartStats:     ChartStats          | null = null;
  chartYMin = 0;
  chartYMax = 30;

  loadingWarehouse = false;
  loadingChart     = false;

  periodForm = this.fb.group({
    preset:   this.fb.nonNullable.control<PeriodPreset>('LAST_7D'),
    fromDate: this.fb.control<Date | null>(null),
    toDate:   this.fb.control<Date | null>(null),
  });

  ngOnInit(): void {
    this.periodForm.controls.preset.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((preset) => this.applyPreset(preset));

    this.applyPreset('LAST_7D');

    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const id = params.get('id');
      if (id) this.loadWarehouse(id);
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Warehouse loading ─────────────────────────────────────────────────────
  private loadWarehouse(id: string): void {
    this.loadingWarehouse = true;
    this.warehouse        = null;
    this.selectedSector   = null;
    this.chartData        = null;
    this.chartStats       = null;

    this.warehouseService.getById(id)
      .pipe(finalize(() => (this.loadingWarehouse = false)))
      .subscribe({
        next: (w) => (this.warehouse = w),
        error: () => this.snack.open('Failed to load warehouse.', 'Close', { duration: 3500 }),
      });
  }

  openImage(imageUrl: string): void {
    const data: ImagePreviewDialogData = { imageUrl, title: this.warehouse?.name ?? 'Warehouse image' };
    this.dialog.open(ImagePreviewDialogComponent, {
      width: 'min(96vw, 1100px)',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: false,
      data,
    });
  }

  // ── Sector selection ──────────────────────────────────────────────────────
  chooseSector(sector: WarehouseSectorDto): void {
    this.selectedSector = sector;
    this.chartData      = null;
    this.chartStats     = null;
  }

  // ── Chart loading ─────────────────────────────────────────────────────────
  loadChart(): void {
    if (!this.warehouse || !this.selectedSector || this.loadingChart) return;

    const preset = this.periodForm.controls.preset.value;
    let from: string | undefined;
    let to:   string | undefined;

    if (preset === 'CUSTOM') {
      const range = this.getCustomRange();
      if (!range) return;
      from = range.from;
      to   = range.to;
    }

    this.loadingChart = true;
    this.analytics
      .getTemperatureChart(this.warehouse.id, this.selectedSector.id, preset, from, to)
      .pipe(finalize(() => (this.loadingChart = false)))
      .subscribe({
        next: (data) => {
          this.chartData = data;
          this.computeChartProps();
        },
        error: () => this.snack.open('Failed to load temperature chart.', 'Close', { duration: 3500 }),
      });
  }

  // ── Chart computation ─────────────────────────────────────────────────────
  private computeChartProps(): void {
    if (!this.chartData?.dataPoints.length) { this.chartStats = null; return; }
    const temps = this.chartData.dataPoints.map((p) => p.temperature);
    const min = Math.min(...temps);
    const max = Math.max(...temps);
    this.chartStats = { min, max, avg: temps.reduce((a, b) => a + b, 0) / temps.length };
    const range = Math.max(4, max - min);
    const pad   = range * 0.15;
    this.chartYMin = Math.floor(min - pad);
    this.chartYMax = Math.ceil(max + pad);
  }

  temperatureLinePoints(): string {
    const pts = this.chartData?.dataPoints;
    if (!pts?.length) return '';
    const step = pts.length > 1 ? (PLOT_RIGHT - PLOT_LEFT) / (pts.length - 1) : 0;
    return pts
      .map((p, i) => `${(PLOT_LEFT + step * i).toFixed(1)},${this.tempToY(p.temperature).toFixed(1)}`)
      .join(' ');
  }

  temperatureAreaPath(): string {
    const pts = this.chartData?.dataPoints;
    if (!pts?.length) return '';
    const step  = pts.length > 1 ? (PLOT_RIGHT - PLOT_LEFT) / (pts.length - 1) : 0;
    const first = PLOT_LEFT;
    const last  = PLOT_LEFT + step * (pts.length - 1);
    const line  = pts
      .map((p, i) => `${(PLOT_LEFT + step * i).toFixed(1)},${this.tempToY(p.temperature).toFixed(1)}`)
      .join(' L ');
    return `M ${first},${PLOT_BOTTOM} L ${line} L ${last},${PLOT_BOTTOM} Z`;
  }

  tempToY(temp: number): number {
    const range = this.chartYMax - this.chartYMin || 1;
    return PLOT_BOTTOM - ((temp - this.chartYMin) / range) * (PLOT_BOTTOM - PLOT_TOP);
  }

  yGridValues(): number[] {
    const step  = this.yGridStep();
    const start = Math.ceil(this.chartYMin / step) * step;
    const vals: number[] = [];
    for (let t = start; t <= this.chartYMax; t += step) vals.push(t);
    return vals;
  }

  private yGridStep(): number {
    const range = this.chartYMax - this.chartYMin;
    if (range <= 8)  return 2;
    if (range <= 20) return 5;
    if (range <= 50) return 10;
    return 20;
  }

  tickIndices(): number[] {
    const count = this.chartData?.dataPoints.length ?? 0;
    if (!count) return [];
    if (count <= 8) return Array.from({ length: count }, (_, i) => i);
    const step    = Math.ceil(count / 8);
    const indices = new Set<number>();
    for (let i = 0; i < count; i += step) indices.add(i);
    indices.add(count - 1);
    return Array.from(indices).sort((a, b) => a - b);
  }

  tickX(index: number): number {
    const count = this.chartData?.dataPoints.length ?? 0;
    if (count <= 1) return PLOT_LEFT;
    return PLOT_LEFT + ((PLOT_RIGHT - PLOT_LEFT) / (count - 1)) * index;
  }

  tickLabel(iso: string): string {
    const d  = new Date(iso);
    const wp = this.chartData?.windowPeriod ?? '1d';
    if (wp === '1mo') return d.toLocaleDateString('en', { month: 'short', year: '2-digit' });
    return d.toLocaleDateString('en', { month: 'short', day: 'numeric' });
  }

  // ── Period helpers ────────────────────────────────────────────────────────
  private applyPreset(preset: PeriodPreset): void {
    if (preset === 'CUSTOM') return;
    const to   = new Date();
    const from = new Date(to.getTime());
    if (preset === 'LAST_7D')  from.setDate(from.getDate() - 7);
    if (preset === 'LAST_30D') from.setDate(from.getDate() - 30);
    if (preset === 'LAST_3M')  from.setMonth(from.getMonth() - 3);
    if (preset === 'LAST_6M')  from.setMonth(from.getMonth() - 6);
    if (preset === 'LAST_1Y')  from.setFullYear(from.getFullYear() - 1);
    from.setHours(0, 0, 0, 0);
    to.setHours(23, 59, 59, 999);
    this.periodForm.patchValue({ fromDate: from, toDate: to }, { emitEvent: false });
  }

  private getCustomRange(): { from: string; to: string } | null {
    const { fromDate, toDate } = this.periodForm.getRawValue();
    if (!fromDate || !toDate) {
      this.snack.open('Select both start and end dates.', 'Close', { duration: 3000 });
      return null;
    }
    const from = new Date(fromDate); from.setHours(0, 0, 0, 0);
    const to   = new Date(toDate);   to.setHours(23, 59, 59, 999);
    if (from >= to) {
      this.snack.open('Start date must be before end date.', 'Close', { duration: 3000 });
      return null;
    }
    if ((to.getTime() - from.getTime()) / ONE_DAY_MS > 366) {
      this.snack.open('Date range cannot exceed one year.', 'Close', { duration: 3500 });
      return null;
    }
    return { from: from.toISOString(), to: to.toISOString() };
  }

  // ── Formatting helpers ────────────────────────────────────────────────────
  formatTemp(t: number | null | undefined): string {
    return t == null ? '—' : `${t.toFixed(1)} °C`;
  }

  cityLabel(w: WarehouseDto): string {
    return w.city ? `${w.city.name}, ${w.city.countryName}` : '—';
  }

  trackById   = (_: number, x: { id: string })           => x.id;
  trackByTime = (_: number, x: TemperatureDataPointDto)  => x.time;
}
