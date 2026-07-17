import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface RejectCompanyRequestDialogData {
  companyName: string;
}

@Component({
  selector: 'app-reject-company-request-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './reject-company-request-dialog.component.html',
  styleUrl: './reject-company-request-dialog.component.scss',
})
export class RejectCompanyRequestDialogComponent {
  private readonly fb = inject(FormBuilder);
  readonly dialogRef = inject(MatDialogRef<RejectCompanyRequestDialogComponent, string>);
  readonly data = inject<RejectCompanyRequestDialogData>(MAT_DIALOG_DATA);

  form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(500)]],
  });

  submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.dialogRef.close(this.form.controls.reason.value.trim());
  }
}
