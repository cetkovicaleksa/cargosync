import { DatePipe, DecimalPipe, NgFor, NgIf } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Subject, finalize, takeUntil } from 'rxjs';

import { VanService } from '../../service/van.service';
import { VanDto } from '../../dto/van.dto';
import { VanStatusDto } from '../../dto/van-status.dto';
import { VanDistanceSeriesDto } from '../../dto/van-distance-series.dto';
import { ProtectedImageComponent } from '../../../shared/util/protected-image.component';
import { ImagePreviewDialogComponent } from '../../../shared/dialog/image-preview-dialog/image-preview-dialog.component';

const PLOT_WIDTH = 840;
const PLOT_HEIGHT = 260;
const PLOT_LEFT = 24;
const PLOT_RIGHT = PLOT_WIDTH - 16;
const PLOT_TOP = 12;
const PLOT_BOTTOM = PLOT_HEIGHT - 58;

type DistancePeriodPreset = '7d' | '1m' | '3m' | '6m' | '1y' | 'custom';

@Component({
  selector: 'app-van-overview',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    DatePipe,
    DecimalPipe,
    NgFor,
    NgIf,
    ProtectedImageComponent,
  ],
  templateUrl: './van-overview.component.html',
  styleUrl: './van-overview.component.scss',
})
export class VanOverviewComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vans = inject(VanService);
  private readonly fb = inject(FormBuilder);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly destroy$ = new Subject<void>();

  van: VanDto | null = null;
  status: VanStatusDto | null = null;
  distanceSeries: VanDistanceSeriesDto | null = null;

  loadingVan = false;
  loadingStatus = false;
  loadingDistance = false;

  vin = '';

  distanceChartMax = 1;
  timeOptions = this.buildTimeOptions();

  periodForm = this.fb.nonNullable.group({
    preset: ['7d' as DistancePeriodPreset],
    fromDate: [this.daysAgo(7)],
    toDate: [new Date()],
  });

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const vin = params.get('vin') ?? '';
      if (!vin || vin === this.vin) {
        if (vin) this.loadAll();
        return;
      }
      this.vin = vin;
      this.loadAll();
    });

    this.periodForm.controls.preset.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((preset) => {
      this.applyPreset(preset);
      this.loadDistanceSeries();
    });
  }

  private loadAll() {
    if (!this.vin) return;
    this.loadVan();
    this.loadStatus();
    this.loadDistanceSeries();
  }

  private loadVan() {
    this.loadingVan = true;
    this.vans.getByVin(this.vin)
      .pipe(finalize(() => (this.loadingVan = false)))
      .subscribe({
        next: (van) => (this.van = van),
        error: () => this.snack.open('Failed to load van details.', 'Close', { duration: 3500 }),
      });
  }

  private loadStatus() {
    this.loadingStatus = true;
    this.vans.getStatus(this.vin)
      .pipe(finalize(() => (this.loadingStatus = false)))
      .subscribe({
        next: (status) => (this.status = status),
        error: () => this.snack.open('Failed to load van status.', 'Close', { duration: 3500 }),
      });
  }

  loadDistanceSeries() {
    if (!this.vin) return;

    const from = this.periodForm.controls.fromDate.value;
    const to = this.periodForm.controls.toDate.value;
    if (!from || !to || from >= to) {
      this.snack.open('Invalid date range.', 'Close', { duration: 3000 });
      return;
    }

    this.loadingDistance = true;
    this.vans.getDistanceSeries(this.vin, from.toISOString(), to.toISOString())
      .pipe(finalize(() => (this.loadingDistance = false)))
      .subscribe({
        next: (series) => {
          this.distanceSeries = series;
          this.distanceChartMax = Math.max(1, ...(series.points ?? []).map((point) => point.distance));
        },
        error: () => this.snack.open('Failed to load distance chart.', 'Close', { duration: 3500 }),
      });
  }

  refresh() {
    this.loadAll();
  }

  openPreview(imageUrl: string) {
    if (!imageUrl) return;

    this.dialog.open(ImagePreviewDialogComponent, {
      width: 'min(96vw, 1100px)',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: false,
      data: { imageUrl, title: 'Van image' },
    });
  }

  back() {
    this.router.navigate(['/vans']);
  }

  presetLabel(): string {
    return this.periodForm.controls.preset.value;
  }

  distanceBarHeight(distance: number): number {
    const ratio = distance / this.distanceChartMax;
    return Math.max(8, Math.round((PLOT_BOTTOM - PLOT_TOP) * ratio));
  }

  distancePointLabel(index: number): string {
    const point = this.distanceSeries?.points?.[index];
    if (!point) return '';
    const start = new Date(point.periodStart);
    return start.toLocaleDateString();
  }

  distanceTickIndices(): number[] {
    const points = this.distanceSeries?.points ?? [];
    if (!points.length) return [];

    const step = Math.max(1, Math.ceil(points.length / 8));
    const indices: number[] = [];
    for (let i = 0; i < points.length; i += step) {
      indices.push(i);
    }
    if (indices[indices.length - 1] !== points.length - 1) {
      indices.push(points.length - 1);
    }
    return indices;
  }

  formatTime(value: string | null | undefined): string {
    if (!value) return '—';
    return new Date(value).toLocaleString();
  }

  formatLocation(): string {
    if (!this.status?.lastSeenAt) return '—';
    return `${this.status.lastSeenAt.latitude}, ${this.status.lastSeenAt.longitude}`;
  }

  openPreviewFromVan(index: number) {
    const image = this.van?.images?.[index];
    if (image) this.openPreview(image);
  }

  trackByIndex = (index: number) => index;

  private applyPreset(preset: DistancePeriodPreset) {
    const now = new Date();
    const from = new Date(now);

    switch (preset) {
      case '7d':
        from.setDate(from.getDate() - 7);
        break;
      case '1m':
        from.setMonth(from.getMonth() - 1);
        break;
      case '3m':
        from.setMonth(from.getMonth() - 3);
        break;
      case '6m':
        from.setMonth(from.getMonth() - 6);
        break;
      case '1y':
        from.setFullYear(from.getFullYear() - 1);
        break;
      case 'custom':
        return;
    }

    this.periodForm.controls.fromDate.setValue(from, { emitEvent: false });
    this.periodForm.controls.toDate.setValue(now, { emitEvent: false });
  }

  private buildTimeOptions() {
    return [
      { key: '7d', label: 'Last 7 days' },
      { key: '1m', label: 'Last month' },
      { key: '3m', label: 'Last 3 months' },
      { key: '6m', label: 'Last 6 months' },
      { key: '1y', label: 'Last year' },
      { key: 'custom', label: 'Custom range' },
    ] as const;
  }

  private daysAgo(days: number): Date {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return date;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
