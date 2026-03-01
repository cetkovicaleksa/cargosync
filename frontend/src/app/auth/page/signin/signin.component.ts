import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../service/auth.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-signin',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './signin.component.html',
  styleUrl: './signin.component.scss',
})
export class SignInComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  loading = false;

  form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  submit() {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    const { username, password } = this.form.getRawValue();

    this.auth.login({ username, password })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          this.router.navigate(['/']);
        },
        error: (err) => {
          let msg: string;
          if (err?.error?.error === 'CredentialsExpired') {
            msg = 'You need to change your password.';
            this.router.navigate(['/password-change'], { queryParams: { username } });
          } else if (err?.error?.error === 'AccountInactive') {
            msg = 'Your account is not activated yet. Check your email for the activation link.';
          } else {
            msg = err?.status === 403
              ? 'Access denied. Your account may be blocked.'
              : 'Sign in failed. Please check your credentials.';
          }
          this.snack.open(msg, 'Close', { duration: 4000 });
        },
      });
  }
}
