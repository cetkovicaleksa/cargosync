import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-force-password-change',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './force-password-change.component.html',
  styleUrl: './force-password-change.component.scss',
})
export class ForcePasswordChangeComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  loading = false;

  form = this.fb.nonNullable.group({
    oldPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmNewPassword: ['', [Validators.required]],
  });

  submit() {
    if (this.form.invalid || this.loading) return;

    const { oldPassword, newPassword, confirmNewPassword } = this.form.getRawValue();
    if (newPassword !== confirmNewPassword) {
      this.snack.open('New passwords do not match.', 'Close', { duration: 3500 });
      return;
    }

    this.loading = true;
    this.auth.changePassword(oldPassword, newPassword)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          this.snack.open('Password updated successfully.', 'Close', { duration: 2500 });
          this.auth.logout();
          this.router.navigate(['/home']);
        },
        error: () => {
          this.snack.open('Password change failed. Please verify your old password.', 'Close', {
            duration: 4000,
          });
        },
      });
  }
}
