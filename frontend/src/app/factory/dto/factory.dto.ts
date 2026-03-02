import { GeoLocationDto } from '../../shared/dto/geo-location.dto';

export interface FactoryDto {
  id: string;
  name: string;
  location: GeoLocationDto;
  street: string;
  cityCode: string;
  cityName: string;
  countryCode: string;
  countryName: string;
  imageUrls: string[];
  version: number;
}
