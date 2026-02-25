import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { ManagerService } from '../../services/manager.service';

@Component({
  selector: 'app-manager-create',
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
  templateUrl: './manager-create.component.html',
  styleUrl: './manager-create.component.scss',
})
export class ManagerCreateComponent {
  private readonly fb = inject(FormBuilder);
  private readonly managers = inject(ManagerService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  loading = false;

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    username: ['', [Validators.required, Validators.minLength(3)]],
    avatarUrl: ['', [Validators.required]],
  });

  submit() {
    if (this.form.invalid || this.loading) return;

    this.loading = true;
    const { email, username, avatarUrl } = this.form.getRawValue();

    this.managers.createManager({ email, username, avatarUrl })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: () => {
          this.snack.open('Manager created successfully.', 'Close', { duration: 3000 });
          this.router.navigate(['/managers']);
        },
        error: () => this.snack.open('Failed to create manager.', 'Close', { duration: 3500 }),
      });
  }
}
