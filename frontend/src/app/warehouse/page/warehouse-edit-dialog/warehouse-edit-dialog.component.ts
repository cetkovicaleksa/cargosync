import { AfterViewInit, Component, ElementRef, inject, OnDestroy, ViewChild } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NgFor, NgIf } from '@angular/common';
import { finalize, Subject, takeUntil } from 'rxjs';
import * as L from 'leaflet';

import { CLIMATE_ZONES, WarehouseDto } from '../../dto/warehouse.dto';
import { CityOptionDto, CountryOptionDto, WarehouseService } from '../../service/warehouse.service';

export type WarehouseEditDialogData =
  | { mode: 'create' }
  | { mode: 'edit'; warehouse: WarehouseDto };

// Fix default marker icons broken by Webpack asset hashing
const iconDefault = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

@Component({
  selector: 'app-warehouse-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatIconModule,
    MatSnackBarModule,
    NgFor,
    NgIf,
  ],
  templateUrl: './warehouse-edit-dialog.component.html',
  styleUrl: './warehouse-edit-dialog.component.scss',
})
export class WarehouseEditDialogComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer') mapContainer!: ElementRef<HTMLDivElement>;

  private readonly fb = inject(FormBuilder);
  private readonly warehouseService = inject(WarehouseService);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy$ = new Subject<void>();

  readonly ref = inject(MatDialogRef<WarehouseEditDialogComponent>);
  readonly data = inject<WarehouseEditDialogData>(MAT_DIALOG_DATA);

  readonly climateZones = CLIMATE_ZONES;

  countries: CountryOptionDto[] = [];
  cities: CityOptionDto[] = [];
  loading = false;
  newImageFiles: File[] = [];
  removedImages = new Set<string>();

  private map!: L.Map;
  private marker: L.Marker | null = null;

  form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    address: ['', Validators.required],
    countryCode: ['', Validators.required],
    cityCode: [{ value: '', disabled: true }, Validators.required],
    latitude: [null as number | null],
    longitude: [null as number | null],
    sectors: this.fb.array<ReturnType<typeof this.createSectorGroup>>([]),
  });

  constructor() {
    if (this.data.mode === 'edit') {
      const w = this.data.warehouse;
      this.form.patchValue({
        name: w.name,
        address: w.address,
        countryCode: w.city?.countryCode ?? '',
        latitude: w.location?.latitude ?? null,
        longitude: w.location?.longitude ?? null,
      });
      w.sectors?.forEach(s =>
        this.sectorsArray.push(this.createSectorGroup(s.name, s.climateZone ?? ''))
      );
    }

    this.warehouseService.getCountries().subscribe({
      next: countries => { this.countries = countries; },
      error: () => this.snack.open('Failed to load countries.', 'Close', { duration: 3000 }),
    });

    const initialCountry = this.form.controls.countryCode.value;
    if (initialCountry) {
      this.form.controls.cityCode.enable({ emitEvent: false });
      this.loadCities(initialCountry, this.data.mode === 'edit' ? (this.data.warehouse.city?.code ?? '') : '');
    }

    this.form.controls.countryCode.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(countryCode => {
        this.form.controls.cityCode.setValue('');
        if (!countryCode) {
          this.cities = [];
          this.form.controls.cityCode.disable({ emitEvent: false });
          return;
        }
        this.form.controls.cityCode.enable({ emitEvent: false });
        this.loadCities(countryCode, '');
      });
  }

  private loadCities(countryCode: string, preselectCode: string) {
    this.warehouseService.getCities(countryCode).subscribe({
      next: cities => {
        this.cities = cities;
        if (preselectCode) {
          this.form.controls.cityCode.setValue(preselectCode);
        }
      },
      error: () => this.snack.open('Failed to load cities.', 'Close', { duration: 3000 }),
    });
  }

  ngAfterViewInit() {
    const initLat = this.form.controls.latitude.value ?? 44.8;
    const initLon = this.form.controls.longitude.value ?? 20.5;
    const hasCoords = this.form.controls.latitude.value != null;

    this.map = L.map(this.mapContainer.nativeElement, { zoomControl: true }).setView(
      [initLat, initLon],
      hasCoords ? 13 : 6
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(this.map);

    if (hasCoords) {
      this.placeMarker(L.latLng(initLat, initLon));
    }

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.placeMarker(e.latlng);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.map?.remove();
  }

  private placeMarker(latlng: L.LatLng) {
    if (this.marker) {
      this.marker.setLatLng(latlng);
    } else {
      this.marker = L.marker(latlng, { icon: iconDefault, draggable: true }).addTo(this.map);
      this.marker.on('dragend', () => {
        const pos = this.marker!.getLatLng();
        this.setCoords(pos.lat, pos.lng);
      });
    }
    this.setCoords(latlng.lat, latlng.lng);
  }

  private setCoords(lat: number, lng: number) {
    this.form.controls.latitude.setValue(parseFloat(lat.toFixed(6)));
    this.form.controls.longitude.setValue(parseFloat(lng.toFixed(6)));
  }

  get title() {
    return this.data.mode === 'create' ? 'New warehouse' : 'Edit warehouse';
  }

  get sectorsArray(): FormArray {
    return this.form.controls.sectors;
  }

  get existingImages(): string[] {
    return this.data.mode === 'edit' ? (this.data.warehouse.images ?? []) : [];
  }

  createSectorGroup(name = '', climateZone = '') {
    return this.fb.nonNullable.group({
      name: [name, Validators.required],
      climateZone: [climateZone],
    });
  }

  addSector() {
    this.sectorsArray.push(this.createSectorGroup());
  }

  removeSector(index: number) {
    this.sectorsArray.removeAt(index);
  }

  onImagesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    this.newImageFiles = [...this.newImageFiles, ...files];
    input.value = '';
  }

  removeNewImage(index: number) {
    this.newImageFiles = this.newImageFiles.filter((_, i) => i !== index);
  }

  toggleRemoveImage(path: string) {
    if (this.removedImages.has(path)) {
      this.removedImages.delete(path);
    } else {
      this.removedImages.add(path);
    }
  }

  isMarkedForRemoval(path: string): boolean {
    return this.removedImages.has(path);
  }

  get latLonLabel(): string {
    const lat = this.form.controls.latitude.value;
    const lon = this.form.controls.longitude.value;
    if (lat == null || lon == null) return 'Click on the map to set location';
    return `${lat.toFixed(5)}, ${lon.toFixed(5)}`;
  }

  save() {
    if (this.form.invalid || this.loading) return;

    const v = this.form.getRawValue();
    const formData = new FormData();

    formData.append('name', v.name);
    formData.append('address', v.address);
    formData.append('countryCode', v.countryCode);
    formData.append('cityCode', v.cityCode);
    if (v.latitude != null) formData.append('latitude', String(v.latitude));
    if (v.longitude != null) formData.append('longitude', String(v.longitude));

    formData.append('sectors', JSON.stringify(
      v.sectors.map(s => ({ name: s.name, climateZone: s.climateZone || null }))
    ));

    this.newImageFiles.forEach(f => formData.append('images', f, f.name));

    if (this.data.mode === 'edit') {
      this.removedImages.forEach(path => formData.append('removeImages', path));
    }

    this.loading = true;

    const request$ = this.data.mode === 'create'
      ? this.warehouseService.create(formData)
      : this.warehouseService.update(this.data.warehouse.id, formData);

    request$
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: saved => this.ref.close(saved),
        error: err => {
          const msg = err?.status === 409
            ? 'Conflict: ' + (err?.error?.message ?? 'Cannot save warehouse.')
            : 'Failed to save warehouse.';
          this.snack.open(msg, 'Close', { duration: 4000 });
        },
      });
  }
}
