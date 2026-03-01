export type ClimateZone = 'AMBIENT' | 'REFRIGERATED' | 'FROZEN' | 'CONTROLLED' | 'HAZARDOUS';

export const CLIMATE_ZONES: { value: ClimateZone; label: string }[] = [
  { value: 'AMBIENT',      label: 'Ambient (15–25°C)' },
  { value: 'REFRIGERATED', label: 'Refrigerated (2–8°C)' },
  { value: 'FROZEN',       label: 'Frozen (≤ −18°C)' },
  { value: 'CONTROLLED',   label: 'Controlled (electronics/sensitive)' },
  { value: 'HAZARDOUS',    label: 'Hazardous (flammable/ventilated)' },
];

export interface WarehouseSectorDto {
  id: string;
  name: string;
  climateZone: ClimateZone | null;
  lastTemperature: number | null;
  lastTemperatureAt: string | null;
}

export interface WarehouseDto {
  id: string;
  name: string;
  address: string;
  city: {
    countryCode: string;
    countryName: string;
    code: string;
    name: string;
    locode: string;
  } | null;
  location: {
    latitude: number;
    longitude: number;
  } | null;
  images: string[];
  online: boolean;
  lastHeartbeat: string | null;
  sectors: WarehouseSectorDto[];
}
