export type AvailabilityBucket = 'hour' | 'day' | 'week' | 'month';

export interface FactoryAvailabilityPointDto {
  periodStart: string;
  periodEnd: string;
  onlineSeconds: number;
  offlineSeconds: number;
  onlinePercent: number;
  offlinePercent: number;
}

export interface FactoryAvailabilitySeriesDto {
  factoryId: string;
  bucket: AvailabilityBucket;
  totalOnlineSeconds: number;
  totalOfflineSeconds: number;
  onlinePercent: number;
  offlinePercent: number;
  points: FactoryAvailabilityPointDto[];
}

export interface FactoryAvailabilityRealtimeEventDto {
  timestamp: string;
  online: boolean;
}
