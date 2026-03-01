import { OrderItemDto } from './order-item.dto';

export type OrderStatus = 'CONFIRMED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderDto {
  id: string;
  companyId: string;
  companyName: string;
  companyAddress: string;
  status: OrderStatus;
  totalAmount: number;
  items: OrderItemDto[];
  createdDate: string;
}
