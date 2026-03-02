export interface OrderItemDto {
  id: string;
  productId: string;
  productName: string;
  productImageUrl: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}
