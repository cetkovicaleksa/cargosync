import { Component, OnDestroy, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DecimalPipe, NgFor, NgIf } from '@angular/common';

import { FactoryService } from '../../service/factory.service';
import { FactoryDto } from '../../dto/factory.dto';
import { CityOptionDto } from '../../dto/city-option.dto';
import { CountryOptionDto } from '../../dto/country-option.dto';
import { FactoryEditDialogComponent, FactoryEditDialogData } from '../factory-edit-dialog/factory-edit-dialog.component';
import { ConfirmDeleteDialogComponent } from '../../../shared/dialog/confirm-delete-dialog/confirm-delete-dialog.component';

@Component({
  selector: 'app-factory-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSnackBarModule,
    MatDialogModule,
    DecimalPipe,
    NgFor,
    NgIf,
  ],
  templateUrl: './factory-list.component.html',
  styleUrl: './factory-list.component.scss',
})
export class FactoryListComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly factories = inject(FactoryService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  private readonly destroy$ = new Subject<void>();

  page = 0;
  size = 20;
  last = false;
  loading = false;
  loadingMore = false;

  items: FactoryDto[] = [];
  countries: CountryOptionDto[] = [];
  cities: CityOptionDto[] = [];

  form = this.fb.nonNullable.group({
    name: [''],
    countryCode: [''],
    cityCode: [''],
  });

  constructor() {
    this.factories.getCountries().subscribe({
      next: (countries) => {
        this.countries = countries ?? [];
      },
      error: () => this.snack.open('Failed to load countries.', 'Close', { duration: 3000 }),
    });

    this.form.controls.countryCode.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((countryCode) => {
        this.form.controls.cityCode.setValue('');
        if (!countryCode) {
          this.cities = [];
          return;
        }

        this.factories.getCities(countryCode).subscribe({
          next: (cities) => {
            this.cities = cities ?? [];
          },
          error: () => this.snack.open('Failed to load cities.', 'Close', { duration: 3000 }),
        });
      });

    this.form.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)), takeUntil(this.destroy$))
      .subscribe(() => this.reload());

    this.reload();
  }

  private getFilter() {
    const v = this.form.getRawValue();
    return {
      name: v.name || null,
      countryCode: v.countryCode || null,
      cityCode: v.cityCode || null,
    };
  }

  reload() {
    if (this.loading) return;

    this.loading = true;
    this.items = [];
    this.page = 0;
    this.last = false;

    this.factories.search(this.getFilter(), 0, this.size)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (p) => {
          this.items = p.content ?? [];
          this.page = p.page?.number ?? 0;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load factories.', 'Close', { duration: 3500 }),
      });
  }

  loadMore() {
    if (this.loadingMore || this.last) return;

    this.loadingMore = true;
    const nextPage = this.page + 1;

    this.factories.search(this.getFilter(), nextPage, this.size)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: (p) => {
          this.items = [...this.items, ...(p.content ?? [])];
          this.page = p.page?.number ?? nextPage;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more factories.', 'Close', { duration: 3500 }),
      });
  }

  openCreate() {
    const data: FactoryEditDialogData = { mode: 'create' };
    this.dialog.open(FactoryEditDialogComponent, {
      width: '860px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    }).afterClosed().subscribe((created: FactoryDto | undefined) => {
      if (created) this.reload();
    });
  }

  openEdit(factory: FactoryDto) {
    const data: FactoryEditDialogData = { mode: 'edit', factory };
    this.dialog.open(FactoryEditDialogComponent, {
      width: '860px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    }).afterClosed().subscribe((updated: FactoryDto | undefined) => {
      if (!updated) return;
      const i = this.items.findIndex((x) => x.id === updated.id);
      if (i >= 0) this.items[i] = updated;
    });
  }

  delete(factory: FactoryDto) {
    this.dialog.open(ConfirmDeleteDialogComponent, {
      width: '420px',
      maxWidth: '96vw',
      autoFocus: false,
      data: {
        title: 'Delete factory',
        message: `Delete factory "${factory.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;

      this.factories.delete(factory.id).subscribe({
        next: () => {
          this.items = this.items.filter((x) => x.id !== factory.id);
          this.snack.open('Factory deleted.', 'Close', { duration: 2500 });
        },
        error: () => this.snack.open('Failed to delete factory.', 'Close', { duration: 3500 }),
      });
    });
  }

  trackById = (_: number, x: FactoryDto) => x.id;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
