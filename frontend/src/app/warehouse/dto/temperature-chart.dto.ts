export type TemperatureChartPeriod = 'LAST_7D' | 'LAST_30D' | 'LAST_3M' | 'LAST_6M' | 'LAST_1Y' | 'CUSTOM';

export interface TemperatureDataPointDto {
  time: string;
  temperature: number;
}

export interface TemperatureChartDto {
  sectorId: string;
  sectorName: string;
  lastTemperature: number | null;
  lastTemperatureAt: string | null;
  windowPeriod: string;
  rangeStart: string;
  rangeStop: string;
  dataPoints: TemperatureDataPointDto[];
}
