import { Component, inject, OnDestroy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ScrollingModule } from '@angular/cdk/scrolling';

import { ManagerService } from '../../services/manager.service';
import { AccountDto } from '../../../auth/dto/account.dto';

type ManagerVm = AccountDto & { _locallyBlocked?: boolean };

@Component({
  selector: 'app-manager-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    ScrollingModule,
  ],
  templateUrl: './manager-list.component.html',
  styleUrl: './manager-list.component.scss',
})
export class ManagerListComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly managers = inject(ManagerService);
  private readonly snack = inject(MatSnackBar);

  private readonly destroy$ = new Subject<void>();

  form = this.fb.nonNullable.group({
    query: [''],
  });

  items: ManagerVm[] = [];
  loading = false;
  loadingMore = false;

  page = 0;
  size = 25;
  last = false;

  constructor() {
    this.form.controls.query.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.reload());

    this.reload();
  }

  reload() {
    if (this.loading) return;

    this.loading = true;
    this.items = [];
    this.page = 0;
    this.last = false;

    const query = this.form.controls.query.value ?? '';

    this.managers.getManagers(query, 0, this.size)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (p) => {
          this.items = (p.content ?? []) as ManagerVm[];
          this.page = p.number ?? 0;
          this.last = !!p.last;
        },
        error: () => this.snack.open('Failed to load managers.', 'Close', { duration: 3500 }),
      });
  }

  loadMore() {
    if (this.loadingMore || this.last) return;

    this.loadingMore = true;
    const nextPage = this.page + 1;
    const query = this.form.controls.query.value ?? '';

    this.managers.getManagers(query, nextPage, this.size)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: (p) => {
          this.items = [...this.items, ...((p.content ?? []) as ManagerVm[])];
          this.page = p.number ?? nextPage;
          this.last = !!p.last;
        },
        error: () => this.snack.open('Failed to load more managers.', 'Close', { duration: 3500 }),
      });
  }

  onViewportIndexChange(lastVisibleIndex: number) {
    if (lastVisibleIndex >= this.items.length - 6) {
      this.loadMore();
    }
  }

  toggleBlock(item: ManagerVm) {
    if (item._locallyBlocked) return;

    this.managers.toggleBlock(item.username).subscribe({
      next: () => {
        item._locallyBlocked = true;
        this.snack.open('Manager block state toggled.', 'Close', { duration: 2500 });
      },
      error: () => this.snack.open('Failed to toggle block.', 'Close', { duration: 3500 }),
    });
  }

  trackByUsername = (_: number, x: ManagerVm) => x.username;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
