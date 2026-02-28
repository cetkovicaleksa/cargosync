export interface SaveProductRequestDto {
  name: string;
  imageUrl: string | null;
  price: number;
  weight: number;
  isForSale: boolean;
  categoryName: string;
  factoryIds: string[]; // UUID[]
  version: number | null;
}
