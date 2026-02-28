import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, finalize, map, of, startWith, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
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

export type ProductEditDialogData =
  | { mode: 'create' }
  | { mode: 'edit'; product: ProductDto };

@Component({
  selector: 'app-product-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatAutocompleteModule,
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
export class ProductEditDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly products = inject(ProductService);
  private readonly snack = inject(MatSnackBar);
  private readonly imageUpload = inject(ImageUploadService);
  private readonly categories = inject(ProductCategoryService);

  readonly ref = inject(MatDialogRef<ProductEditDialogComponent>);
  readonly data = inject<ProductEditDialogData>(MAT_DIALOG_DATA);

  loading = false;
  selectedImageFile: File | null = null;
  categoryOptions: string[] = [];
  filteredCategoryOptions: Observable<string[]> = of([]);

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    price: [0, [Validators.required, Validators.min(0)]],
    weight: [0, [Validators.required, Validators.min(0)]],
    isForSale: [false],
    categoryName: ['', [Validators.required]],
    factoryIdsCsv: [''],
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
        factoryIdsCsv: (p.factoryIds ?? []).join(', '),
      });
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

  private parseFactoryIds(csv: string): string[] {
    return (csv ?? '')
      .split(',')
      .map(x => x.trim())
      .filter(x => x.length > 0);
  }

  private filterCategoryOptions(value: string): string[] {
    const q = (value || '').toLowerCase().trim();
    if (!q) return this.categoryOptions;
    return this.categoryOptions.filter((x) => x.toLowerCase().includes(q));
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
      factoryIds: this.parseFactoryIds(v.factoryIdsCsv),
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
}
