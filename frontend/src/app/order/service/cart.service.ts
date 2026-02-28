import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ProductDto } from '../../product/dto/product.dto';

export interface CartItem {
  product: ProductDto;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _items = new BehaviorSubject<Map<string, CartItem>>(new Map());
  readonly items$ = this._items.asObservable();

  get snapshot(): CartItem[] {
    return Array.from(this._items.value.values());
  }

  get count(): number {
    return this._items.value.size;
  }

  get total(): number {
    return this.snapshot.reduce((sum, i) => sum + i.product.price * i.quantity, 0);
  }

  add(product: ProductDto, quantity = 1): void {
    const map = new Map(this._items.value);
    const existing = map.get(product.id);
    const maxStock = product.totalStock ?? Infinity;
    const newQty = Math.min((existing?.quantity ?? 0) + quantity, maxStock);
    map.set(product.id, { product, quantity: newQty });
    this._items.next(map);
  }

  setQuantity(productId: string, quantity: number): void {
    const map = new Map(this._items.value);
    const item = map.get(productId);
    if (!item) return;
    if (quantity <= 0) {
      map.delete(productId);
    } else {
      map.set(productId, { ...item, quantity });
    }
    this._items.next(map);
  }

  remove(productId: string): void {
    const map = new Map(this._items.value);
    map.delete(productId);
    this._items.next(map);
  }

  clear(): void {
    this._items.next(new Map());
  }
}
