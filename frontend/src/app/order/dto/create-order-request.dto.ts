export interface CreateOrderRequestDto {
  companyId: string;
  items: { productId: string; quantity: number }[];
}
