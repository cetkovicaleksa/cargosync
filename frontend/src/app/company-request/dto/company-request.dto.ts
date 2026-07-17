import { GeoLocationDto } from '../../shared/dto/geo-location.dto';

export type CompanyRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface CompanyRequestCityDto {
  countryCode: string;
  countryName?: string;
  code: string;
  name?: string;
}

export interface CompanyRequestDto {
  id: string;
  name: string;
  address: string;
  city: CompanyRequestCityDto;
  location: GeoLocationDto;
  status: CompanyRequestStatus;
  images: string[];
  proofs: string[];
  version?: number;
}
