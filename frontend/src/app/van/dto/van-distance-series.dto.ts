import { VanDistancePointDto } from './van-distance-point.dto';

export interface VanDistanceSeriesDto {
  vanVin: string;
  bucket: string;
  totalDistance: number;
  points: VanDistancePointDto[];
}
