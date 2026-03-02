import { Component, inject, OnDestroy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, Subject, takeUntil } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NgFor, NgIf } from '@angular/common';

import { AuthService } from '../../../auth/service/auth.service';
import { CityOptionDto, CountryOptionDto, WarehouseService } from '../../service/warehouse.service';
import { WarehouseDto } from '../../dto/warehouse.dto';
import { WarehouseEditDialogComponent, WarehouseEditDialogData } from '../warehouse-edit-dialog/warehouse-edit-dialog.component';
import { ConfirmDeleteDialogComponent } from '../../../shared/dialog/confirm-delete-dialog/confirm-delete-dialog.component';

@Component({
  selector: 'app-warehouse-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    NgFor,
    NgIf,
  ],
  templateUrl: './warehouse-list.component.html',
  styleUrl: './warehouse-list.component.scss',
})
export class WarehouseListComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly warehouseService = inject(WarehouseService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);

  private readonly destroy$ = new Subject<void>();

  readonly canManage = () => this.auth.hasAuthority('ROLE_MANAGER');

  countries: CountryOptionDto[] = [];
  cities: CityOptionDto[] = [];

  page = 0;
  size = 20;
  last = false;
  loading = false;
  loadingMore = false;

  items: WarehouseDto[] = [];

  form = this.fb.group({
    name: [''],
    countryCode: [''],
    cityCode: [{ value: '', disabled: true }],
    online: [null as boolean | null],
  });

  constructor() {
    this.warehouseService.getCountries().subscribe({
      next: countries => { this.countries = countries; },
      error: () => this.snack.open('Failed to load countries.', 'Close', { duration: 3000 }),
    });

    this.form.controls.countryCode.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(countryCode => {
        this.form.controls.cityCode.setValue('', { emitEvent: false });
        this.cities = [];
        if (countryCode) {
          this.form.controls.cityCode.enable({ emitEvent: false });
          this.warehouseService.getCities(countryCode).subscribe({
            next: cities => { this.cities = cities; },
            error: () => this.snack.open('Failed to load cities.', 'Close', { duration: 3000 }),
          });
        } else {
          this.form.controls.cityCode.disable({ emitEvent: false });
        }
      });

    this.form.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        takeUntil(this.destroy$),
      )
      .subscribe(() => this.reload());

    this.reload();
  }

  private getFilter() {
    const v = this.form.getRawValue();
    return {
      name: v.name || null,
      countryCode: v.countryCode || null,
      cityCode: v.cityCode || null,
      online: v.online,
    };
  }

  reload() {
    if (this.loading) return;
    this.loading = true;
    this.items = [];
    this.page = 0;
    this.last = false;

    this.warehouseService.list(this.getFilter(), 0, this.size)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: p => {
          this.items = p.content ?? [];
          this.page = p.page?.number ?? 0;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load warehouses.', 'Close', { duration: 3500 }),
      });
  }

  loadMore() {
    if (this.loadingMore || this.last) return;
    this.loadingMore = true;
    const nextPage = this.page + 1;

    this.warehouseService.list(this.getFilter(), nextPage, this.size)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: p => {
          this.items = [...this.items, ...(p.content ?? [])];
          this.page = p.page?.number ?? nextPage;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more warehouses.', 'Close', { duration: 3500 }),
      });
  }

  openCreate() {
    const data: WarehouseEditDialogData = { mode: 'create' };
    this.dialog.open(WarehouseEditDialogComponent, {
      width: '760px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    }).afterClosed().subscribe((created: WarehouseDto | null | undefined) => {
      if (created) this.reload();
    });
  }

  openEdit(w: WarehouseDto) {
    const data: WarehouseEditDialogData = { mode: 'edit', warehouse: w };
    this.dialog.open(WarehouseEditDialogComponent, {
      width: '760px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    }).afterClosed().subscribe((updated: WarehouseDto | null | undefined) => {
      if (!updated) return;
      const i = this.items.findIndex(x => x.id === updated.id);
      if (i >= 0) this.items[i] = updated;
    });
  }

  delete(w: WarehouseDto) {
    this.dialog.open(ConfirmDeleteDialogComponent, {
      width: '420px',
      maxWidth: '96vw',
      autoFocus: false,
      data: {
        title: 'Delete warehouse',
        message: `Delete warehouse "${w.name}"? This will fail if the warehouse has active inventory.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;
      this.warehouseService.delete(w.id).subscribe({
        next: () => {
          this.items = this.items.filter(x => x.id !== w.id);
          this.snack.open('Warehouse deleted.', 'Close', { duration: 2500 });
        },
        error: err => {
          const msg = err?.status === 409
            ? 'Cannot delete: warehouse has active inventory.'
            : 'Failed to delete warehouse.';
          this.snack.open(msg, 'Close', { duration: 4000 });
        },
      });
    });
  }

  goToDetail(w: WarehouseDto): void {
    this.router.navigate(['/warehouses', w.id]);
  }

  cityLabel(w: WarehouseDto): string {
    if (!w.city) return '—';
    return `${w.city.countryCode} ${w.city.name}`;
  }

  trackById = (_: number, x: WarehouseDto) => x.id;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
