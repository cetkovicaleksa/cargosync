import {
  AfterViewInit,
  Component,
  ElementRef,
  Inject,
  OnDestroy,
  ViewChild,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NgFor, NgIf } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import * as L from 'leaflet';

import { FactoryDto } from '../../dto/factory.dto';
import { FactoryService } from '../../service/factory.service';
import { CountryOptionDto } from '../../dto/country-option.dto';
import { CityOptionDto } from '../../dto/city-option.dto';
import { SaveFactoryRequestDto } from '../../dto/save-factory-request.dto';
import { ImagePreviewDialogComponent } from '../../../shared/dialog/image-preview-dialog/image-preview-dialog.component';
import { ProtectedImageComponent } from "../../../shared/util/protected-image.component";
import { CountryService } from '../../../shared/service/country.service';

const leafletIconRetinaUrl = new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).toString();
const leafletIconUrl = new URL('leaflet/dist/images/marker-icon.png', import.meta.url).toString();
const leafletShadowUrl = new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).toString();

L.Icon.Default.mergeOptions({
  iconRetinaUrl: leafletIconRetinaUrl,
  iconUrl: leafletIconUrl,
  shadowUrl: leafletShadowUrl,
});

type SelectedImagePreview = {
  file: File;
  url: string;
};

export type FactoryEditDialogData =
  | {
      mode: 'create';
    }
  | {
      mode: 'edit';
      factory: FactoryDto;
    };

@Component({
  selector: 'app-factory-edit-dialog',
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
    NgIf,
    NgFor,
    ProtectedImageComponent
],
  templateUrl: './factory-edit-dialog.component.html',
  styleUrl: './factory-edit-dialog.component.scss',
})
export class FactoryEditDialogComponent implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly factories = inject(FactoryService);
  private readonly countryService = inject(CountryService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<FactoryEditDialogComponent, FactoryDto | undefined>);
  private readonly dialog = inject(MatDialog);

  private readonly destroy$ = new Subject<void>();

  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  countries: CountryOptionDto[] = [];
  cities: CityOptionDto[] = [];

  existingImageUrls: string[] = [];
  selectedImagePreviews: SelectedImagePreview[] = [];

  submitting = false;
  private map?: L.Map;
  private marker?: L.Marker;

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    street: ['', [Validators.required, Validators.maxLength(160)]],
    countryCode: ['', [Validators.required]],
    cityCode: ['', [Validators.required]],
    latitude: [45.2671, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [19.8335, [Validators.required, Validators.min(-180), Validators.max(180)]],
  });

  constructor(@Inject(MAT_DIALOG_DATA) public data: FactoryEditDialogData) {
    if (data.mode === 'edit') {
      this.form.patchValue({
        name: data.factory.name,
        street: data.factory.street,
        countryCode: data.factory.countryCode,
        cityCode: data.factory.cityCode,
        latitude: data.factory.location.latitude,
        longitude: data.factory.location.longitude,
      });
      this.existingImageUrls = [...(data.factory.imageUrls ?? [])];
    }

    this.countryService.getCountries().subscribe({
      next: (countries) => {
        this.countries = countries ?? [];
      },
      error: () => this.snack.open('Failed to load countries.', 'Close', { duration: 3000 }),
    });

    const initialCountry = this.form.controls.countryCode.value;
    if (initialCountry) {
      this.loadCities(initialCountry);
    }

    this.form.controls.countryCode.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((countryCode) => {
        this.form.controls.cityCode.setValue('');
        if (!countryCode) {
          this.cities = [];
          return;
        }
        this.loadCities(countryCode);
      });

    this.form.controls.latitude.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.updateMarkerFromForm());
    this.form.controls.longitude.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.updateMarkerFromForm());
  }

  get title() {
    return this.data.mode === 'create' ? 'Create Factory' : 'Edit Factory';
  }

  private loadCities(countryCode: string) {
    this.countryService.getCities(countryCode).subscribe({
      next: (cities) => {
        this.cities = cities ?? [];

        if (this.data.mode === 'edit' && this.data.factory.countryCode === countryCode) {
          this.form.controls.cityCode.setValue(this.data.factory.cityCode);
        }
      },
      error: () => this.snack.open('Failed to load cities.', 'Close', { duration: 3000 }),
    });
  }

  ngAfterViewInit(): void {
    if (!this.mapHost) return;

    this.map = L.map(this.mapHost.nativeElement, {
      center: [this.form.controls.latitude.value, this.form.controls.longitude.value],
      zoom: 6,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);

    this.marker = L.marker([this.form.controls.latitude.value, this.form.controls.longitude.value], {
      draggable: false,
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      const { lat, lng } = event.latlng;
      this.form.patchValue({ latitude: Number(lat.toFixed(6)), longitude: Number(lng.toFixed(6)) });
      this.updateMarkerFromForm();
    });

    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private updateMarkerFromForm() {
    if (!this.marker || !this.map) return;

    const lat = this.form.controls.latitude.value;
    const lng = this.form.controls.longitude.value;
    if (lat == null || lng == null) return;

    this.marker.setLatLng([lat, lng]);
  }

  onPickFiles(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (files.length === 0) return;

    const allowed = new Set(['image/jpeg', 'image/png', 'image/webp']);
    const valid = files.filter((file) => allowed.has(file.type) && file.size <= 10 * 1024 * 1024);

    if (valid.length !== files.length) {
      this.snack.open('Only JPG, PNG, or WebP files up to 10 MB are allowed.', 'Close', { duration: 3500 });
    }

    const additions = valid.map((file) => ({ file, url: URL.createObjectURL(file) }));
    this.selectedImagePreviews = [...this.selectedImagePreviews, ...additions];
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

  openPreview(imageUrl: string) {
    if (!imageUrl) return;
    this.dialog.open(ImagePreviewDialogComponent, {
      width: 'min(96vw, 1100px)',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: false,
      data: { imageUrl, title: 'Factory image' },
    });
  }

  submit() {
    if (this.submitting) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.submitting = true;

    const selectedFiles = this.selectedImagePreviews.map((x) => x.file);
    const value = this.form.getRawValue();

    const request: SaveFactoryRequestDto = {
      name: value.name.trim(),
      street: value.street.trim(),
      countryCode: value.countryCode,
      cityCode: value.cityCode,
      imageUrls: [...this.existingImageUrls],
      location: {
        latitude: Number(value.latitude),
        longitude: Number(value.longitude),
      },
      version: this.data.mode === 'edit' ? this.data.factory.version : null,
    };

    const request$ = this.data.mode === 'create'
      ? this.factories.create(request, selectedFiles)
      : this.factories.update(this.data.factory.id, request, selectedFiles);

    request$
      .subscribe({
        next: (saved) => {
          this.submitting = false;
          this.snack.open(this.data.mode === 'create' ? 'Factory created successfully.' : 'Factory updated successfully.', 'Close', {
            duration: 2800,
          });
          this.dialogRef.close(saved);
        },
        error: (err) => {
          this.submitting = false;
          const message = err?.error?.message ?? 'Failed to save factory.';
          this.snack.open(message, 'Close', { duration: 3500 });
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.map?.remove();
    this.selectedImagePreviews.forEach((x) => URL.revokeObjectURL(x.url));
  }
}
