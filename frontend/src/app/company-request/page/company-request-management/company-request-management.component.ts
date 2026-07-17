import { Component, OnDestroy, inject } from '@angular/core';
import { DecimalPipe, NgFor, NgIf } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { finalize, Subject, switchMap, takeUntil } from 'rxjs';

import { CompanyRequestDto, CompanyRequestStatus } from '../../dto/company-request.dto';
import { CompanyRequestService } from '../../service/company-request.service';
import { ProtectedImageComponent } from '../../../shared/util/protected-image.component';
import { ImagePreviewDialogComponent } from '../../../shared/dialog/image-preview-dialog/image-preview-dialog.component';
import { ConfirmDeleteDialogComponent } from '../../../shared/dialog/confirm-delete-dialog/confirm-delete-dialog.component';
import {
  RejectCompanyRequestDialogComponent,
  RejectCompanyRequestDialogData,
} from '../reject-company-request-dialog/reject-company-request-dialog.component';

@Component({
  selector: 'app-company-request-management',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatDialogModule,
    MatSnackBarModule,
    MatIconModule,
    NgIf,
    NgFor,
    DecimalPipe,
    ProtectedImageComponent,
  ],
  templateUrl: './company-request-management.component.html',
  styleUrl: './company-request-management.component.scss',
})
export class CompanyRequestManagementComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly companyRequests = inject(CompanyRequestService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly destroy$ = new Subject<void>();

  page = 0;
  size = 10;
  last = false;
  loading = false;
  loadingMore = false;

  items: CompanyRequestDto[] = [];

  form = this.fb.nonNullable.group({
    status: ['PENDING' as CompanyRequestStatus],
  });

  constructor() {
    this.form.controls.status.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => this.reload());
    this.reload();
  }

  reload() {
    if (this.loading) return;

    this.loading = true;
    this.page = 0;
    this.last = false;
    this.items = [];

    this.companyRequests
      .list(this.form.controls.status.value, 0, this.size)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (p) => {
          this.items = p.content ?? [];
          this.page = p.page?.number ?? 0;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load company requests.', 'Close', { duration: 3500 }),
      });
  }

  loadMore() {
    if (this.loadingMore || this.last) return;

    this.loadingMore = true;
    const nextPage = this.page + 1;

    this.companyRequests
      .list(this.form.controls.status.value, nextPage, this.size)
      .pipe(finalize(() => (this.loadingMore = false)))
      .subscribe({
        next: (p) => {
          this.items = [...this.items, ...(p.content ?? [])];
          this.page = p.page?.number ?? nextPage;
          this.last = !p.page || p.page.totalPages === 0 || this.page >= p.page.totalPages - 1;
        },
        error: () => this.snack.open('Failed to load more requests.', 'Close', { duration: 3500 }),
      });
  }

  approve(request: CompanyRequestDto) {
    this.dialog
      .open(ConfirmDeleteDialogComponent, {
        width: '420px',
        maxWidth: '96vw',
        autoFocus: false,
        data: {
          title: 'Approve request',
          message: `Approve company registration request for "${request.name}"?`,
          confirmText: 'Approve',
          cancelText: 'Cancel',
        },
      })
      .afterClosed()
      .subscribe((confirmed: boolean | undefined) => {
        if (!confirmed) return;

        this.companyRequests
          .getByIdWithEtag(request.id)
          .pipe(switchMap(({ etag }) => this.companyRequests.approve(request.id, etag)))
          .subscribe({
            next: () => {
              this.snack.open('Request approved.', 'Close', { duration: 2800 });
              this.reload();
            },
            error: (err) => {
              const message = err?.status === 409
                ? 'Request changed by another manager. Refresh and try again.'
                : err?.error?.message ?? 'Failed to approve request.';
              this.snack.open(message, 'Close', { duration: 4200 });
            },
          });
      });
  }

  reject(request: CompanyRequestDto) {
    const data: RejectCompanyRequestDialogData = { companyName: request.name };

    this.dialog
      .open(RejectCompanyRequestDialogComponent, {
        width: '560px',
        maxWidth: '96vw',
        autoFocus: false,
        data,
      })
      .afterClosed()
      .subscribe((reason: string | undefined) => {
        if (!reason) return;

        this.companyRequests
          .getByIdWithEtag(request.id)
          .pipe(switchMap(({ etag }) => this.companyRequests.reject(request.id, etag, { reason })))
          .subscribe({
            next: () => {
              this.snack.open('Request rejected.', 'Close', { duration: 2800 });
              this.reload();
            },
            error: (err) => {
              const message = err?.status === 409
                ? 'Request changed by another manager. Refresh and try again.'
                : err?.error?.message ?? 'Failed to reject request.';
              this.snack.open(message, 'Close', { duration: 4200 });
            },
          });
      });
  }

  openImage(path: string) {
    this.dialog.open(ImagePreviewDialogComponent, {
      width: 'min(96vw, 1100px)',
      maxWidth: '96vw',
      maxHeight: '94vh',
      autoFocus: false,
      data: { imageUrl: path, title: 'Attachment preview' },
    });
  }

  openProof(path: string) {
    if (this.isImage(path)) {
      this.openImage(path);
      return;
    }

    this.companyRequests.getAttachmentBlob(path).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank', 'noopener,noreferrer');
        setTimeout(() => URL.revokeObjectURL(url), 15_000);
      },
      error: () => this.snack.open('Failed to open document.', 'Close', { duration: 3000 }),
    });
  }

  isImage(path: string): boolean {
    return /\.(png|jpe?g|gif|webp)$/i.test(path);
  }

  cityLabel(request: CompanyRequestDto): string {
    const cityName = request.city?.name ?? request.city?.code ?? 'Unknown city';
    const country = request.city?.countryName ?? request.city?.countryCode ?? 'Unknown country';
    return `${cityName}, ${country}`;
  }

  trackById = (_: number, item: CompanyRequestDto) => item.id;

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
