import {Component, inject, OnInit} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
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
export class ForcePasswordChangeComponent implements OnInit{
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly activatedRoute = inject(ActivatedRoute);

  loading = false;
  username = '';

  form = this.fb.nonNullable.group({
    oldPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmNewPassword: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.activatedRoute.queryParams.subscribe(params => {
      const username = params['username'];
      if (!username) {
        this.snack.open('Invalid request.', 'Close', { duration: 3500 });
        this.router.navigate(['/']);
        return;
      }
      this.username = username;
    })
  }

  submit() {
    if (this.form.invalid || this.loading) return;

    const { oldPassword, newPassword, confirmNewPassword } = this.form.getRawValue();
    if (newPassword !== confirmNewPassword) {
      this.snack.open('New passwords do not match.', 'Close', { duration: 3500 });
      return;
    }

    this.loading = true;
    this.auth.changePassword(this.username, oldPassword, newPassword)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          this.snack.open('Password updated successfully.', 'Close', { duration: 2500 });
          this.router.navigate(['/']);
        },
        error: () => {
          this.snack.open('Password change failed. Please verify your old password.', 'Close', {
            duration: 4000,
          });
        },
      });
  }
}
