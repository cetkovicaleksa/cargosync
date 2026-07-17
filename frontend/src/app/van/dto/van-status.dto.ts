import { GeoLocationDto } from '../../shared/dto/geo-location.dto';

export interface VanStatusDto {
  vin: string;
  online: boolean;
  lastHeartbeat: string | null;
  lastSeenAt: GeoLocationDto | null;
}
