import { NgFor, NgIf } from '@angular/common';
import { Component, Inject, OnDestroy, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';

import { VanDto } from '../../dto/van.dto';
import { VanService } from '../../service/van.service';
import { MakeDto } from '../../dto/make.dto';
import { ModelDto } from '../../dto/model.dto';
import { SaveVanRequestDto } from '../../dto/save-van-request.dto';
import { ImagePreviewDialogComponent } from '../../../shared/dialog/image-preview-dialog/image-preview-dialog.component';
import { ProtectedImageComponent } from '../../../shared/util/protected-image.component';

export type VanEditDialogData =
  | { mode: 'create' }
  | { mode: 'edit'; van: VanDto };

type SelectedImagePreview = { file: File; url: string };

@Component({
  selector: 'app-van-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    NgFor,
    NgIf,
    ProtectedImageComponent,
  ],
  templateUrl: './van-edit-dialog.component.html',
  styleUrl: './van-edit-dialog.component.scss',
})
export class VanEditDialogComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly vans = inject(VanService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly destroy$ = new Subject<void>();

  readonly dialogRef = inject(MatDialogRef<VanEditDialogComponent, VanDto | undefined>);
  readonly data = inject<VanEditDialogData>(MAT_DIALOG_DATA);

  makes: MakeDto[] = [];
  models: ModelDto[] = [];

  existingImageUrls: string[] = [];
  selectedImagePreviews: SelectedImagePreview[] = [];

  submitting = false;

  form = this.fb.nonNullable.group({
    registrationNumber: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(20)]],
    maxPayload: [0, [Validators.required, Validators.min(1)]],
    makeCode: ['', [Validators.required]],
    modelCode: ['', [Validators.required]],
  });

  constructor() {
    if (this.data.mode === 'edit') {
      this.form.patchValue({
        registrationNumber: this.data.van.registrationNumber,
        maxPayload: this.data.van.maxPayload,
        makeCode: this.data.van.model.wmi,
        modelCode: this.data.van.model.vd,
      });
      this.existingImageUrls = [...(this.data.van.images ?? [])];
    }

    this.vans.getMakes().subscribe({
      next: (makes) => {
        this.makes = makes ?? [];
        this.syncModels(this.form.controls.makeCode.value, this.data.mode === 'edit');
      },
      error: () => this.snack.open('Failed to load makes.', 'Close', { duration: 3000 }),
    });

    this.form.controls.makeCode.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((makeCode) => {
        this.form.controls.modelCode.setValue('');
        this.syncModels(makeCode, false);
      });
  }

  get title() {
    return this.data.mode === 'create' ? 'New van' : 'Edit van';
  }

  get modeLabel() {
    return this.data.mode === 'create' ? 'Create' : 'Save';
  }

  private syncModels(makeCode: string, preserveSelected: boolean) {
    const selectedMake = this.makes.find((make) => make.wmi === makeCode);
    this.models = selectedMake?.models ?? [];

    if (!preserveSelected) {
      return;
    }

    const selectedModel = this.form.controls.modelCode.value;
    if (selectedModel && !this.models.some((model) => model.vd === selectedModel)) {
      this.form.controls.modelCode.setValue('');
    }
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

  onPickFiles(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (files.length === 0) return;

    const allowed = new Set(['image/jpeg', 'image/png', 'image/webp']);
    const valid = files.filter((file) => allowed.has(file.type) && file.size <= 10 * 1024 * 1024);

    if (valid.length !== files.length) {
      this.snack.open('Only JPG, PNG, or WebP images up to 10 MB are allowed.', 'Close', { duration: 3500 });
    }

    this.selectedImagePreviews = [
      ...this.selectedImagePreviews,
      ...valid.map((file) => ({ file, url: URL.createObjectURL(file) })),
    ];
    input.value = '';
  }

  removeSelectedFile(index: number) {
    const removed = this.selectedImagePreviews[index];
    if (removed) {
      URL.revokeObjectURL(removed.url);
    }
    this.selectedImagePreviews = this.selectedImagePreviews.filter((_, i) => i !== index);
  }

  removeExistingImage(index: number) {
    this.existingImageUrls = this.existingImageUrls.filter((_, i) => i !== index);
  }

  submit() {
    if (this.submitting) return;

    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    if (this.data.mode === 'create' && this.selectedImagePreviews.length === 0) {
      this.snack.open('Please upload at least one van image.', 'Close', { duration: 3500 });
      return;
    }

    const value = this.form.getRawValue();
    const request: SaveVanRequestDto = {
      registrationNumber: value.registrationNumber.trim(),
      maxPayload: Number(value.maxPayload),
      model: {
        wmi: value.makeCode,
        vd: value.modelCode,
      },
      version: this.data.mode === 'edit' ? this.data.van.version : null,
    };

    const selectedFiles = this.selectedImagePreviews.map((item) => item.file);
    const removedImages = this.data.mode === 'edit'
      ? (this.data.van.images ?? []).filter((url) => !this.existingImageUrls.includes(url))
      : [];

    this.submitting = true;

    const request$ = this.data.mode === 'create'
      ? this.vans.create(request, selectedFiles)
      : this.vans.update(this.data.van.vin, request, selectedFiles, removedImages);

    request$.subscribe({
      next: (saved) => {
        this.submitting = false;
        this.snack.open(this.data.mode === 'create' ? 'Van created.' : 'Van updated.', 'Close', { duration: 2800 });
        this.dialogRef.close(saved);
      },
      error: (err) => {
        this.submitting = false;
        const message = err?.error?.message ?? 'Failed to save van.';
        this.snack.open(message, 'Close', { duration: 4000 });
      },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.selectedImagePreviews.forEach((item) => URL.revokeObjectURL(item.url));
  }
}
