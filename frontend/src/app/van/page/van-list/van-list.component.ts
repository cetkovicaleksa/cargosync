import { NgFor, NgIf, DecimalPipe } from '@angular/common';
import { Component, OnDestroy, inject } from '@angular/core';
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

import { AuthService } from '../../../auth/service/auth.service';
import { ConfirmDeleteDialogComponent } from '../../../shared/dialog/confirm-delete-dialog/confirm-delete-dialog.component';
import { ImagePreviewDialogComponent } from '../../../shared/dialog/image-preview-dialog/image-preview-dialog.component';
import { ProtectedImageComponent } from '../../../shared/util/protected-image.component';
import { VanService } from '../../service/van.service';
import { VanDto } from '../../dto/van.dto';
import { MakeDto } from '../../dto/make.dto';
import { ModelDto } from '../../dto/model.dto';
import { VanEditDialogComponent, VanEditDialogData } from '../van-edit-dialog/van-edit-dialog.component';

@Component({
  selector: 'app-van-list',
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
    DecimalPipe,
    NgFor,
    NgIf,
    ProtectedImageComponent,
  ],
  templateUrl: './van-list.component.html',
  styleUrl: './van-list.component.scss',
})
export class VanListComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly vans = inject(VanService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  readonly canManage = () => this.auth.hasAuthority('ROLE_MANAGER');

  items: VanDto[] = [];
  makes: MakeDto[] = [];
  models: ModelDto[] = [];

  page = 0;
  size = 20;
  last = false;
  loading = false;
  loadingMore = false;

  form = this.fb.nonNullable.group({
    q: [''],
    make: [''],
    model: [''],
    canCarryKg: [null as number | null],
    tooBigIfCanCarryKg: [null as number | null],
    sortBy: ['registrationNumber'],
    sortDirection: ['asc' as 'asc' | 'desc'],
  });

  constructor() {
    this.vans.getMakes().subscribe({
      next: (makes) => {
        this.makes = makes ?? [];
        this.syncModels(this.form.controls.make.value);
      },
      error: () => this.snack.open('Failed to load van makes.', 'Close', { duration: 3000 }),
    });

    this.form.controls.make.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((make) => {
      this.form.controls.model.setValue('');
      this.syncModels(make);
    });

    this.form.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)), takeUntil(this.destroy$))
      .subscribe(() => this.reload());

    this.reload();
  }

  private syncModels(makeCode: string) {
    const selectedMake = this.makes.find((make) => make.wmi === makeCode);
    this.models = selectedMake?.models ?? [];
  }

  private getFilter() {
    const v = this.form.getRawValue();
    return {
      q: v.q || null,
      make: v.make || null,
      model: v.model || null,
      canCarryKg: v.canCarryKg,
      tooBigIfCanCarryKg: v.tooBigIfCanCarryKg,
    };
  }

  reload() {
    if (this.loading) return;

    this.loading = true;
    this.items = [];
    this.page = 0;
    this.last = false;

    this.vans.search(this.getFilter(), 0, this.size, this.form.controls.sortBy.value, this.form.controls.sortDirection.value)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (page) => {
          this.items = page.content ?? [];
          this.page = page.page?.number ?? 0;
          this.last = !page.page || page.page.totalPages === 0 || this.page >= page.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load vans.', 'Close', { duration: 3500 }),
      });
  }

  loadMore() {
    if (this.loadingMore || this.last) return;

    this.loadingMore = true;
    const nextPage = this.page + 1;

    this.vans.search(this.getFilter(), nextPage, this.size, this.form.controls.sortBy.value, this.form.controls.sortDirection.value)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: (page) => {
          this.items = [...this.items, ...(page.content ?? [])];
          this.page = page.page?.number ?? nextPage;
          this.last = !page.page || page.page.totalPages === 0 || this.page >= page.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more vans.', 'Close', { duration: 3500 }),
      });
  }

  openCreate() {
    const data: VanEditDialogData = { mode: 'create' };
    this.dialog.open(VanEditDialogComponent, {
      width: '760px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    }).afterClosed().subscribe((created: VanDto | null | undefined) => {
      if (created) this.reload();
    });
  }

  openEdit(van: VanDto) {
    const data: VanEditDialogData = { mode: 'edit', van };
    this.dialog.open(VanEditDialogComponent, {
      width: '760px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    }).afterClosed().subscribe((updated: VanDto | null | undefined) => {
      if (!updated) return;
      const index = this.items.findIndex((item) => item.vin === updated.vin);
      if (index >= 0) this.items[index] = updated;
    });
  }

  delete(van: VanDto) {
    if (!this.canManage()) return;

    this.dialog.open(ConfirmDeleteDialogComponent, {
      width: '420px',
      maxWidth: '96vw',
      autoFocus: false,
      data: {
        title: 'Delete van',
        message: `Delete van "${van.registrationNumber}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;

      this.vans.delete(van.vin).subscribe({
        next: () => {
          this.items = this.items.filter((item) => item.vin !== van.vin);
          this.snack.open('Van deleted.', 'Close', { duration: 2500 });
        },
        error: () => this.snack.open('Failed to delete van.', 'Close', { duration: 3500 }),
      });
    });
  }

  openOverview(van: VanDto) {
    this.router.navigate(['/vans', van.vin]);
  }

  openImagePreview(imageUrl: string) {
    if (!imageUrl) return;

    this.dialog.open(ImagePreviewDialogComponent, {
      width: 'min(96vw, 1100px)',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: false,
      data: { imageUrl, title: 'Van image' },
    });
  }

  modelLabel(van: VanDto): string {
    return `${van.model.name} (${van.model.wmi} / ${van.model.vd})`;
  }

  imageUrl(van: VanDto): string | null {
    return van.images?.[0] ?? null;
  }

  imageCount(van: VanDto): number {
    return van.images?.length ?? 0;
  }

  trackByVin = (_: number, item: VanDto) => item.vin;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
