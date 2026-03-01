import { Component, inject, OnDestroy } from '@angular/core';
import { Subject, finalize } from 'rxjs';
import { DecimalPipe, NgFor, NgIf, DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';

import { OrderService } from '../../service/order.service';
import { OrderDto } from '../../dto/order.dto';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [
    DecimalPipe,
    DatePipe,
    NgFor,
    NgIf,
    MatButtonModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatExpansionModule,
    MatIconModule,
  ],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
})
export class OrderListComponent implements OnDestroy {
  private readonly orderService = inject(OrderService);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy$ = new Subject<void>();

  page = 0;
  size = 20;
  last = false;
  loading = false;
  loadingMore = false;

  orders: OrderDto[] = [];

  constructor() {
    this.reload();
  }

  reload(): void {
    if (this.loading) return;
    this.loading = true;
    this.orders = [];
    this.page = 0;
    this.last = false;

    this.orderService.getMine(0, this.size)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (p) => {
          this.orders = p.content ?? [];
          this.page = p.page?.number ?? 0;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load orders.', 'Close', { duration: 3500 }),
      });
  }

  loadMore(): void {
    if (this.loadingMore || this.last) return;
    this.loadingMore = true;
    const nextPage = this.page + 1;

    this.orderService.getMine(nextPage, this.size)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: (p) => {
          this.orders = [...this.orders, ...(p.content ?? [])];
          this.page = p.page?.number ?? nextPage;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more.', 'Close', { duration: 3500 }),
      });
  }

  trackById = (_: number, o: OrderDto) => o.id;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
