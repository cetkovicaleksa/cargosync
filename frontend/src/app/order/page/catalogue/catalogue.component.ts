import { Component, inject, OnDestroy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ProductService } from '../../../product/service/product.service';
import { ProductCategoryService } from '../../../product/service/product-category.service';
import { CartService } from '../../service/cart.service';
import { CheckoutDialogComponent } from '../checkout-dialog/checkout-dialog.component';
import { ProductDto } from '../../../product/dto/product.dto';

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatSnackBarModule,
    MatDialogModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    DecimalPipe,
    NgFor,
    NgIf,
  ],
  templateUrl: './catalogue.component.html',
  styleUrl: './catalogue.component.scss',
})
export class CatalogueComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(ProductCategoryService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly cart = inject(CartService);

  private readonly destroy$ = new Subject<void>();

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
    sort: ['name,asc'],
  });

  constructor() {
    this.categoryService.getAllCategoryNames().subscribe({
      next: (names) => {
        this.categoryOptions = names ?? [];
        this.updateCategorySuggestions(this.form.controls.categoryName.value);
      },
    });

    this.form.controls.categoryName.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((v) => this.updateCategorySuggestions(v));

    this.form.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)), takeUntil(this.destroy$))
      .subscribe(() => this.reload());

    this.reload();
  }

  private updateCategorySuggestions(value: string): void {
    const q = (value || '').trim().toLowerCase();
    this.filteredCategoryOptions = q
      ? this.categoryOptions.filter((x) => x.toLowerCase().includes(q))
      : this.categoryOptions;
  }

  reload(): void {
    if (this.loading) return;
    this.loading = true;
    this.items = [];
    this.page = 0;
    this.last = false;

    const filter = this.getFilter();
    this.productService.catalogue(filter, 0, this.size, this.form.controls.sort.value)
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

  loadMore(): void {
    if (this.loadingMore || this.last) return;
    this.loadingMore = true;
    const nextPage = this.page + 1;

    this.productService.catalogue(this.getFilter(), nextPage, this.size, this.form.controls.sort.value)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: (p) => {
          this.items = [...this.items, ...(p.content ?? [])];
          this.page = p.page?.number ?? nextPage;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more.', 'Close', { duration: 3500 }),
      });
  }

  addToCart(product: ProductDto): void {
    if ((product.totalStock ?? 0) <= 0) return;
    this.cart.add(product);
    this.snack.open(`"${product.name}" added to cart.`, 'Close', { duration: 2000 });
  }

  openCheckout(): void {
    if (this.cart.count === 0) return;
    this.dialog.open(CheckoutDialogComponent, {
      width: '520px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
    });
  }

  cartQuantity(product: ProductDto): number {
    return this.cart.snapshot.find(i => i.product.id === product.id)?.quantity ?? 0;
  }

  private getFilter() {
    const v = this.form.getRawValue();
    return {
      name: v.name || null,
      categoryName: v.categoryName || null,
      minPrice: v.minPrice,
      maxPrice: v.maxPrice,
      isForSale: null,
    };
  }

  trackById = (_: number, x: ProductDto) => x.id;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
