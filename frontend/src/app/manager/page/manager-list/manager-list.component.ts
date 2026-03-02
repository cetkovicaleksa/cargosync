import { Component, inject, OnDestroy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { ManagerService } from '../../service/manager.service';
import { AccountDto } from '../../../auth/dto/account.dto';
import { NgFor, NgIf } from '@angular/common';
import { ManagerCreateComponent } from '../manager-create/manager-create.component';
import { ProtectedImageComponent } from "../../../shared/util/protected-image.component";

type ManagerVm = AccountDto & { _locallyBlocked?: boolean };

@Component({
  selector: 'app-manager-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
    MatDialogModule,
    NgFor,
    NgIf,
    ProtectedImageComponent
],
  templateUrl: './manager-list.component.html',
  styleUrl: './manager-list.component.scss',
})
export class ManagerListComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly managers = inject(ManagerService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

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
          this.page = p.page?.number ?? 0;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
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
          this.page = p.page?.number ?? nextPage;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more managers.', 'Close', { duration: 3500 }),
      });
  }

  openCreate() {
    this.dialog.open(ManagerCreateComponent, {
      width: '680px',
      maxWidth: '96vw',
      maxHeight: '92vh',
      autoFocus: false,
      data: { mode: 'create' },
    })
      .afterClosed()
      .subscribe((created: boolean | null | undefined) => {
        if (created) this.reload();
      });
  }

  toggleBlock(item: ManagerVm) {
    if (item._locallyBlocked) return;

    this.managers.toggleBlock(item.username).subscribe({
      next: () => {
        if (item.accountNonLocked)
          this.snack.open('Manager blocked.', 'Close', { duration: 2500 });
        else
          this.snack.open('Manager unblocked.', 'Close', { duration: 2500 });
        item.accountNonLocked = !item.accountNonLocked;
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
