import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  inject,
} from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subject, takeUntil } from 'rxjs';
import * as L from 'leaflet';

import { CountryOptionDto } from '../../../factory/dto/country-option.dto';
import { CityOptionDto } from '../../../factory/dto/city-option.dto';
import { CountryService } from '../../../shared/service/country.service';
import { CompanyRequestService } from '../../service/company-request.service';
import { CreateCompanyRequestDto } from '../../dto/create-company-request.dto';

const leafletIconRetinaUrl = new URL('leaflet/dist/images/marker-icon-2x.png', import.meta.url).toString();
const leafletIconUrl = new URL('leaflet/dist/images/marker-icon.png', import.meta.url).toString();
const leafletShadowUrl = new URL('leaflet/dist/images/marker-shadow.png', import.meta.url).toString();

L.Icon.Default.mergeOptions({
  iconRetinaUrl: leafletIconRetinaUrl,
  iconUrl: leafletIconUrl,
  shadowUrl: leafletShadowUrl,
});

type LocalFilePreview = {
  file: File;
  isImage: boolean;
  previewUrl?: string;
};

@Component({
  selector: 'app-company-request-create',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    NgIf,
    NgFor,
  ],
  templateUrl: './company-request-create.component.html',
  styleUrl: './company-request-create.component.scss',
})
export class CompanyRequestCreateComponent implements AfterViewInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly countryService = inject(CountryService);
  private readonly companyRequests = inject(CompanyRequestService);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy$ = new Subject<void>();

  @ViewChild('mapHost') mapHost?: ElementRef<HTMLDivElement>;

  countries: CountryOptionDto[] = [];
  cities: CityOptionDto[] = [];

  images: LocalFilePreview[] = [];
  proofs: LocalFilePreview[] = [];

  submitting = false;
  private map?: L.Map;
  private marker?: L.Marker;

  form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    address: ['', [Validators.required, Validators.maxLength(160)]],
    countryCode: ['', [Validators.required]],
    cityCode: ['', [Validators.required]],
    latitude: [45.2671, [Validators.required, Validators.min(-90), Validators.max(90)]],
    longitude: [19.8335, [Validators.required, Validators.min(-180), Validators.max(180)]],
  });

  constructor() {
    this.countryService.getCountries().subscribe({
      next: (countries) => (this.countries = countries ?? []),
      error: () => this.snack.open('Failed to load countries.', 'Close', { duration: 3000 }),
    });

    this.form.controls.countryCode.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((countryCode) => {
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
      this.form.patchValue({
        latitude: Number(lat.toFixed(6)),
        longitude: Number(lng.toFixed(6)),
      });
      this.updateMarkerFromForm();
    });

    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private loadCities(countryCode: string) {
    this.countryService.getCities(countryCode).subscribe({
      next: (cities) => (this.cities = cities ?? []),
      error: () => this.snack.open('Failed to load cities.', 'Close', { duration: 3000 }),
    });
  }

  private updateMarkerFromForm() {
    if (!this.marker) return;

    const lat = this.form.controls.latitude.value;
    const lng = this.form.controls.longitude.value;
    this.marker.setLatLng([lat, lng]);
  }

  onPickImages(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (files.length === 0) return;

    const valid = files.filter((file) => file.type.startsWith('image/') && file.size <= 10 * 1024 * 1024);
    if (valid.length !== files.length) {
      this.snack.open('Only image files up to 10 MB are allowed.', 'Close', { duration: 3500 });
    }

    this.images = [...this.images, ...valid.map((file) => this.toPreview(file))].slice(0, 32);
    input.value = '';
  }

  onPickProofs(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    if (files.length === 0) return;

    const valid = files.filter(
      (file) => (file.type.startsWith('image/') || file.type === 'application/pdf') && file.size <= 15 * 1024 * 1024,
    );
    if (valid.length !== files.length) {
      this.snack.open('Proofs must be images or PDF documents up to 15 MB.', 'Close', { duration: 3500 });
    }

    this.proofs = [...this.proofs, ...valid.map((file) => this.toPreview(file))].slice(0, 32);
    input.value = '';
  }

  private toPreview(file: File): LocalFilePreview {
    const isImage = file.type.startsWith('image/');
    return {
      file,
      isImage,
      previewUrl: isImage ? URL.createObjectURL(file) : undefined,
    };
  }

  removeImage(index: number) {
    const removed = this.images[index];
    if (removed?.previewUrl) URL.revokeObjectURL(removed.previewUrl);
    this.images = this.images.filter((_, i) => i !== index);
  }

  removeProof(index: number) {
    const removed = this.proofs[index];
    if (removed?.previewUrl) URL.revokeObjectURL(removed.previewUrl);
    this.proofs = this.proofs.filter((_, i) => i !== index);
  }

  submit() {
    if (this.submitting) return;

    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    if (this.images.length === 0) {
      this.snack.open('Please upload at least one company image.', 'Close', { duration: 3500 });
      return;
    }

    if (this.proofs.length === 0) {
      this.snack.open('Please upload at least one ownership proof image or PDF.', 'Close', { duration: 3500 });
      return;
    }

    const value = this.form.getRawValue();
    const payload: CreateCompanyRequestDto = {
      name: value.name.trim(),
      address: value.address.trim(),
      city: {
        countryCode: value.countryCode,
        code: value.cityCode,
      },
      location: {
        latitude: Number(value.latitude),
        longitude: Number(value.longitude),
      },
    };

    this.submitting = true;

    this.companyRequests
      .create(
        payload,
        this.images.map((x) => x.file),
        this.proofs.map((x) => x.file),
      )
      .subscribe({
        next: () => {
          this.submitting = false;
          this.snack.open('Company registration request submitted.', 'Close', { duration: 3000 });
          this.form.patchValue({
            name: '',
            address: '',
            countryCode: '',
            cityCode: '',
            latitude: 45.2671,
            longitude: 19.8335,
          });
          this.cities = [];
          this.cleanupPreviews();
          this.images = [];
          this.proofs = [];
          this.updateMarkerFromForm();
        },
        error: (err) => {
          this.submitting = false;
          const message = err?.error?.message ?? 'Failed to submit company registration request.';
          this.snack.open(message, 'Close', { duration: 4000 });
        },
      });
  }

  private cleanupPreviews() {
    [...this.images, ...this.proofs].forEach((item) => {
      if (item.previewUrl) URL.revokeObjectURL(item.previewUrl);
    });
  }

  trackByFileName = (_: number, item: LocalFilePreview) => item.file.name;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.map?.remove();
    this.cleanupPreviews();
  }
}
