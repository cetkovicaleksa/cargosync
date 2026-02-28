import { Component, OnDestroy, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, Subject, debounceTime, distinctUntilChanged, finalize, map, of, startWith, switchMap, takeUntil } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AsyncPipe, NgFor, NgIf } from '@angular/common';

import { ProductService } from '../../service/product.service';
import { ProductDto } from '../../dto/product.dto';
import { SaveProductRequestDto } from '../../dto/save-product-request.dto';
import { ImageUploadService } from '../../../shared/service/image-upload.service';
import { ProductCategoryService } from '../../service/product-category.service';
import { FactoryService } from '../../../factory/service/factory.service';
import { FactoryDto } from '../../../factory/dto/factory.dto';

export type ProductEditDialogData =
  | { mode: 'create' }
  | { mode: 'edit'; product: ProductDto };

type SelectedFactory = {
  id: string;
  name: string;
  cityName: string;
  countryCode: string;
};

@Component({
  selector: 'app-product-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatAutocompleteModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    AsyncPipe,
    NgFor,
    NgIf,
  ],
  templateUrl: './product-edit-dialog.component.html',
  styleUrl: './product-edit-dialog.component.scss',
})
export class ProductEditDialogComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly products = inject(ProductService);
  private readonly snack = inject(MatSnackBar);
  private readonly imageUpload = inject(ImageUploadService);
  private readonly categories = inject(ProductCategoryService);
  private readonly factories = inject(FactoryService);

  private readonly destroy$ = new Subject<void>();

  readonly ref = inject(MatDialogRef<ProductEditDialogComponent>);
  readonly data = inject<ProductEditDialogData>(MAT_DIALOG_DATA);

  loading = false;
  selectedImageFile: File | null = null;

  categoryOptions: string[] = [];
  filteredCategoryOptions: Observable<string[]> = of([]);

  factoryQuery = '';
  factoryResults: FactoryDto[] = [];
  selectedFactories: SelectedFactory[] = [];
  factoriesLoading = false;
  factoriesLoadingMore = false;
  factoriesPage = 0;
  factoriesLast = true;

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    price: [0, [Validators.required, Validators.min(0)]],
    weight: [0, [Validators.required, Validators.min(0)]],
    isForSale: [false],
    categoryName: ['', [Validators.required]],
    factorySearch: [''],
  });

  constructor() {
    if (this.data.mode === 'edit') {
      const p = this.data.product;
      this.form.patchValue({
        name: p.name,
        price: p.price,
        weight: p.weight,
        isForSale: p.isForSale,
        categoryName: p.categoryName,
      });

      this.selectedFactories = (p.factoryIds ?? []).map((id) => ({
        id,
        name: id,
        cityName: '',
        countryCode: '',
      }));

      if ((p.factoryIds ?? []).length > 0) {
        this.factories.getByIds(p.factoryIds).subscribe({
          next: (resolved) => {
            const byId = new Map(resolved.map((x) => [x.id, x]));
            this.selectedFactories = (p.factoryIds ?? []).map((id) => {
              const factory = byId.get(id);
              return {
                id,
                name: factory?.name ?? id,
                cityName: factory?.cityName ?? '',
                countryCode: factory?.countryCode ?? '',
              };
            });
          },
          error: () => this.snack.open('Failed to resolve selected factories.', 'Close', { duration: 3000 }),
        });
      }
    }

    this.filteredCategoryOptions = this.form.controls.categoryName.valueChanges.pipe(
      startWith(this.form.controls.categoryName.value),
      map((value) => this.filterCategoryOptions(value)),
    );

    this.categories.getAllCategoryNames().subscribe({
      next: (names) => {
        this.categoryOptions = names ?? [];
        this.filteredCategoryOptions = this.form.controls.categoryName.valueChanges.pipe(
          startWith(this.form.controls.categoryName.value),
          map((value) => this.filterCategoryOptions(value)),
        );
      },
      error: () => this.snack.open('Failed to load categories.', 'Close', { duration: 3000 }),
    });

    this.form.controls.factorySearch.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((value) => this.searchFactories(value || ''));
  }

  get selectedImageName() {
    return this.selectedImageFile?.name ?? '';
  }

  get existingImageUrl() {
    return this.data.mode === 'edit' ? (this.data.product.imageUrl ?? '') : '';
  }

  get title() {
    return this.data.mode === 'create' ? 'New product' : 'Edit product';
  }

  private filterCategoryOptions(value: string): string[] {
    const q = (value || '').toLowerCase().trim();
    if (!q) return this.categoryOptions;
    return this.categoryOptions.filter((x) => x.toLowerCase().includes(q));
  }

  private searchFactories(query: string) {
    this.factoryQuery = query;
    const q = query.trim();

    if (q.length < 2) {
      this.factoryResults = [];
      this.factoriesPage = 0;
      this.factoriesLast = true;
      return;
    }

    this.factoriesLoading = true;
    this.factoriesPage = 0;

    this.factories.search({ name: q }, 0, 15)
      .pipe(finalize(() => (this.factoriesLoading = false)))
      .subscribe({
        next: (p) => {
          this.factoryResults = p.content ?? [];
          this.factoriesPage = p.page?.number ?? 0;
          this.factoriesLast = !p.page || p.page.totalPages === 0 || this.factoriesPage >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to search factories.', 'Close', { duration: 3000 }),
      });
  }

  loadMoreFactories() {
    if (this.factoriesLoadingMore || this.factoriesLast) return;

    const q = this.factoryQuery.trim();
    if (q.length < 2) return;

    const nextPage = this.factoriesPage + 1;
    this.factoriesLoadingMore = true;

    this.factories.search({ name: q }, nextPage, 15)
      .pipe(finalize(() => (this.factoriesLoadingMore = false)))
      .subscribe({
        next: (p) => {
          const existing = new Set(this.factoryResults.map((x) => x.id));
          const appended = (p.content ?? []).filter((x) => !existing.has(x.id));
          this.factoryResults = [...this.factoryResults, ...appended];
          this.factoriesPage = p.page?.number ?? nextPage;
          this.factoriesLast = !p.page || p.page.totalPages === 0 || this.factoriesPage >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more factories.', 'Close', { duration: 3000 }),
      });
  }

  isFactorySelected(id: string): boolean {
    return this.selectedFactories.some((x) => x.id === id);
  }

  addFactory(factory: FactoryDto) {
    if (this.isFactorySelected(factory.id)) return;

    this.selectedFactories = [
      ...this.selectedFactories,
      {
        id: factory.id,
        name: factory.name,
        cityName: factory.cityName,
        countryCode: factory.countryCode,
      },
    ];
  }

  removeFactory(id: string) {
    this.selectedFactories = this.selectedFactories.filter((x) => x.id !== id);
  }

  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedImageFile = input.files?.[0] ?? null;
  }

  save() {
    if (this.form.invalid || this.loading) return;

    const v = this.form.getRawValue();

    const req: SaveProductRequestDto = {
      name: v.name.trim(),
      imageUrl: null,
      price: Number(v.price),
      weight: Number(v.weight),
      isForSale: !!v.isForSale,
      categoryName: v.categoryName.trim(),
      factoryIds: this.selectedFactories.map((x) => x.id),
      version: this.data.mode === 'edit' ? this.data.product.version : null,
    };

    this.loading = true;

    const uploadOrKeepImage$ = this.selectedImageFile
      ? this.imageUpload.uploadImage(
        this.selectedImageFile,
        this.imageUpload.generateRandomImageName(this.selectedImageFile),
      )
      : of(this.data.mode === 'edit' ? this.data.product.imageUrl : null);

    uploadOrKeepImage$
      .pipe(
        switchMap((imageUrl) => {
          req.imageUrl = imageUrl;
          return this.data.mode === 'create'
            ? this.products.create(req)
            : this.products.update(this.data.product.id, req);
        }),
        finalize(() => (this.loading = false)),
      )
      .subscribe({
        next: (saved) => this.ref.close(saved),
        error: (err) => {
          const msg = err?.status === 409
            ? 'This product was updated by someone else. Please refresh and try again.'
            : 'Failed to save product.';
          this.snack.open(msg, 'Close', { duration: 4000 });
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
