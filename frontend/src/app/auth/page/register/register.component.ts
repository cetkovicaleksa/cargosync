import { Component, ElementRef, inject, OnDestroy, ViewChild } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../service/auth.service';
import { finalize } from 'rxjs';
import { NgIf } from '@angular/common';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordsMismatch: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    NgIf,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  loading = false;
  avatarFile: File | null = null;
  avatarPreviewUrl: string | null = null;

  ngOnDestroy() {
    if (this.avatarPreviewUrl) {
      URL.revokeObjectURL(this.avatarPreviewUrl);
    }
  }

  form = this.fb.nonNullable.group(
    {
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatchValidator }
  );

  get firstNameCtrl() { return this.form.get('firstName')!; }
  get lastNameCtrl() { return this.form.get('lastName')!; }
  get emailCtrl() { return this.form.get('email')!; }
  get passwordCtrl() { return this.form.get('password')!; }
  get confirmPasswordCtrl() { return this.form.get('confirmPassword')!; }

  onAvatarClick() {
    this.fileInput.nativeElement.click();
  }

  onFileChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) {
      this.snack.open('Only JPG, PNG, or WebP images are allowed.', 'Close', { duration: 3500 });
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.snack.open('Image must not exceed 10 MB.', 'Close', { duration: 3500 });
      return;
    }
    if (this.avatarPreviewUrl) {
      URL.revokeObjectURL(this.avatarPreviewUrl);
    }
    this.avatarFile = file;
    this.avatarPreviewUrl = URL.createObjectURL(file);
  }

  submit() {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    const { firstName, lastName, email, password, confirmPassword } = this.form.getRawValue();

    this.auth.register({ firstName, lastName, email, password, confirmPassword }, this.avatarFile)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res) => {
          this.snack.open(res.message, 'Close', { duration: 6000 });
          this.router.navigate(['/signin']);
        },
        error: (err) => {
          const msg = err?.error?.message ?? 'Registration failed. Please try again.';
          this.snack.open(msg, 'Close', { duration: 4000 });
        },
      });
  }
}
