export interface VanSearchFilterDto {
  q?: string | null;
  make?: string | null;
  model?: string | null;
  canCarryKg?: number | null;
  tooBigIfCanCarryKg?: number | null;
}
