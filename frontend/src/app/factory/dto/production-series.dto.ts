export type ProductionBucket = 'day' | 'week' | 'month';

export interface ProductionPointDto {
  periodStart: string;
  periodEnd: string;
  units: number;
}

export interface ProductionSeriesDto {
  factoryId: string;
  productId: string;
  bucket: ProductionBucket;
  totalUnits: number;
  points: ProductionPointDto[];
}
