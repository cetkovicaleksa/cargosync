import { ModelDto } from './model.dto';

export interface VanDto {
  vin: string;
  vi: string;
  registrationNumber: string;
  model: ModelDto;
  maxPayload: number;
  images: string[];
  version: number;
}
