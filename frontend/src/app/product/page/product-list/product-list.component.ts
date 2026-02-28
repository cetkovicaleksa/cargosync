import { Component, inject, OnDestroy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ProductService } from '../../service/product.service';
import { ProductDto } from '../../dto/product.dto';
import { AuthService } from '../../../auth/service/auth.service';
import { ProductEditDialogComponent, ProductEditDialogData } from '../product-edit-dialog/product-edit-dialog.component';
import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { ConfirmDeleteDialogComponent } from '../../../shared/dialog/confirm-delete-dialog/confirm-delete-dialog.component';
import { ProductCategoryService } from '../../service/product-category.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
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
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly products = inject(ProductService);
  private readonly categories = inject(ProductCategoryService);
  private readonly auth = inject(AuthService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  private readonly destroy$ = new Subject<void>();

  readonly canManage = () => this.auth.hasAuthority('ROLE_MANAGER');

  page = 0;
  size = 24;
  last = false;
  loading = false;
  loadingMore = false;

  items: ProductDto[] = [];
  categoryOptions: string[] = [];
  filteredCategoryOptions: string[] = [];

  form = this.fb.nonNullable.group({
    name: [''],
    categoryName: [''],
    minPrice: [null as number | null],
    maxPrice: [null as number | null],
    isForSale: [null as boolean | null],
    sort: ['name,asc'],
  });

  constructor() {
    this.categories.getAllCategoryNames().subscribe({
      next: (names) => {
        this.categoryOptions = names ?? [];
        this.updateCategorySuggestions(this.form.controls.categoryName.value);
      },
      error: () => this.snack.open('Failed to load categories.', 'Close', { duration: 3000 }),
    });

    this.form.controls.categoryName.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((value) => this.updateCategorySuggestions(value));

    this.form.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)), takeUntil(this.destroy$))
      .subscribe(() => this.reload());

    this.reload();
  }

  private updateCategorySuggestions(value: string) {
    const query = (value || '').trim().toLowerCase();
    if (!query) {
      this.filteredCategoryOptions = this.categoryOptions;
      return;
    }
    this.filteredCategoryOptions = this.categoryOptions.filter((x) =>
      x.toLowerCase().includes(query),
    );
  }

  private getFilter() {
    const v = this.form.getRawValue();
    return {
      name: v.name || null,
      categoryName: v.categoryName || null,
      minPrice: v.minPrice,
      maxPrice: v.maxPrice,
      isForSale: v.isForSale,
    };
  }

  reload() {
    if (this.loading) return;

    this.loading = true;
    this.items = [];
    this.page = 0;
    this.last = false;

    const filter = this.getFilter();
    const sort = this.form.controls.sort.value;

    this.products.search(filter, 0, this.size, sort)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (p) => {
          this.items = p.content ?? [];
          this.page = p.page?.number ?? 0;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load products.', 'Close', { duration: 3500 }),
      });
  }

  loadMore() {
    if (this.loadingMore || this.last) return;

    this.loadingMore = true;
    const nextPage = this.page + 1;

    const filter = this.getFilter();
    const sort = this.form.controls.sort.value;

    this.products.search(filter, nextPage, this.size, sort)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: (p) => {
          this.items = [...this.items, ...(p.content ?? [])];
          this.page = p.page?.number ?? nextPage;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more products.', 'Close', { duration: 3500 }),
      });
  }

  openCreate() {
    const data: ProductEditDialogData = { mode: 'create' };
    this.dialog.open(ProductEditDialogComponent, {
      width: '720px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    })
      .afterClosed()
      .subscribe((created: ProductDto | null | undefined) => {
        if (created) this.reload();
      });
  }

  openEdit(p: ProductDto) {
    const data: ProductEditDialogData = { mode: 'edit', product: p };
    this.dialog.open(ProductEditDialogComponent, {
      width: '720px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data,
    })
      .afterClosed()
      .subscribe((updated: ProductDto | null | undefined) => {
        if (!updated) return;
        const i = this.items.findIndex(x => x.id === updated.id);
        if (i >= 0) this.items[i] = updated;
      });
  }

  delete(p: ProductDto) {
    if (!this.canManage()) return;

    this.dialog.open(ConfirmDeleteDialogComponent, {
      width: '420px',
      maxWidth: '96vw',
      autoFocus: false,
      data: {
        title: 'Delete product',
        message: `Delete product "${p.name}"?`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
      },
    }).afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;

      this.products.delete(p.id).subscribe({
        next: () => {
          this.items = this.items.filter(x => x.id !== p.id);
          this.snack.open('Product deleted.', 'Close', { duration: 2500 });
        },
        error: () => this.snack.open('Failed to delete product.', 'Close', { duration: 3500 }),
      });
    });
  }

  trackById = (_: number, x: ProductDto) => x.id;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
