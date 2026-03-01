import { GeoLocationDto } from '../../shared/dto/geo-location.dto';

export interface SaveFactoryRequestDto {
  name: string;
  location: GeoLocationDto;
  street: string;
  cityCode: string;
  countryCode: string;
  imageUrls: string[];
  version: number | null;
}
