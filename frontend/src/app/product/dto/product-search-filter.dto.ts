export interface ProductSearchFilterDto {
  name?: string | null;
  categoryName?: string | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  isForSale?: boolean | null;
}
