import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, switchMap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ManagerService } from '../../service/manager.service';
import { ImageUploadService } from '../../../shared/service/image-upload.service';

type ManagerCreateData = { mode: 'create' };

@Component({
  selector: 'app-manager-create',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './manager-create.component.html',
  styleUrl: './manager-create.component.scss',
})
export class ManagerCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly managers = inject(ManagerService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly imageUpload = inject(ImageUploadService);
  readonly dialogRef = inject(MatDialogRef<ManagerCreateComponent>, { optional: true });
  readonly dialogData = inject<ManagerCreateData | null>(MAT_DIALOG_DATA, { optional: true });

  get isDialogMode() {
    return !!this.dialogRef;
  }

  loading = false;
  avatarFile: File | null = null;

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    username: ['', [Validators.required, Validators.minLength(3)]],
  });

  get selectedAvatarName() {
    return this.avatarFile?.name ?? '';
  }

  onAvatarSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    this.avatarFile = input.files?.[0] ?? null;
  }

  submit() {
    if (this.form.invalid || this.loading) return;
    if (!this.avatarFile) {
      this.snack.open('Please select an avatar image.', 'Close', { duration: 3000 });
      return;
    }

    this.loading = true;
    const { email, username } = this.form.getRawValue();

    const imageName = this.imageUpload.generateRandomImageName(this.avatarFile);

    this.imageUpload.uploadImage(this.avatarFile, imageName)
      .pipe(
        switchMap((avatarUrl) => this.managers.createManager({ email, username, avatarUrl })),
        finalize(() => (this.loading = false)),
      )
      .subscribe({
        next: () => {
          this.snack.open('Manager created successfully.', 'Close', { duration: 3000 });
          if (this.dialogRef) {
            this.dialogRef.close(true);
            return;
          }
          this.router.navigate(['/managers']);
        },
        error: () => this.snack.open('Failed to create manager.', 'Close', { duration: 3500 }),
      });
  }
}
