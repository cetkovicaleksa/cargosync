import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DecimalPipe, NgFor } from '@angular/common';
import { CartService } from '../../service/cart.service';
import { OrderService } from '../../service/order.service';
import { CompanyService } from '../../service/company.service';
import { CompanyDto } from '../../dto/company.dto';
import { OrderDto } from '../../dto/order.dto';

@Component({
  selector: 'app-checkout-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    DecimalPipe,
    NgFor,
  ],
  templateUrl: './checkout-dialog.component.html',
  styleUrl: './checkout-dialog.component.scss',
})
export class CheckoutDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cart = inject(CartService);
  private readonly orders = inject(OrderService);
  private readonly companyService = inject(CompanyService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<CheckoutDialogComponent>);

  submitting = false;
  loadingCompanies = true;
  companies: CompanyDto[] = [];

  readonly items = this.cart.snapshot;
  readonly total = this.cart.total;

  form = this.fb.nonNullable.group({
    companyId: ['', Validators.required],
  });

  ngOnInit(): void {
    this.companyService.getMyCompanies()
      .pipe(finalize(() => (this.loadingCompanies = false)))
      .subscribe({
        next: (list) => {
          this.companies = list;
          if (list.length === 1) {
            this.form.controls.companyId.setValue(list[0].id);
          }
        },
        error: () => this.snack.open('Failed to load companies.', 'Close', { duration: 3000 }),
      });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) return;

    this.submitting = true;
    const request = {
      companyId: this.form.controls.companyId.value,
      items: this.items.map(i => ({ productId: i.product.id, quantity: i.quantity })),
    };

    this.orders.create(request)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (order: OrderDto) => {
          this.cart.clear();
          this.snack.open('Order placed! Invoice sent to your email.', 'Close', { duration: 4000 });
          this.dialogRef.close(order);
        },
        error: (err) => {
          const msg = err?.error?.message ?? 'Failed to place order. Please try again.';
          this.snack.open(msg, 'Close', { duration: 5000 });
        },
      });
  }
}
