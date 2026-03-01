export interface ProductDto {
  id: string; // UUID
  name: string;
  imageUrl: string | null;
  price: number;
  weight: number; // Double
  isForSale: boolean;
  categoryName: string;
  factoryIds: string[]; // UUID[]
  version: number; // optimistic locking
  totalStock: number | null; // populated on /catalogue, null on manager CRUD
}
