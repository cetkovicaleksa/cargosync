import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';

type SavePasswordDialogData = {
  password: string;
};

@Component({
  selector: 'app-save-password',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatSnackBarModule, MatIconModule],
  templateUrl: './save-password.component.html',
  styleUrl: './save-password.component.scss'
})
export class SavePasswordComponent {
  private readonly snack = inject(MatSnackBar);
  readonly dialogRef = inject(MatDialogRef<SavePasswordComponent, { copied: boolean }>, { optional: true });
  readonly data = inject<SavePasswordDialogData>(MAT_DIALOG_DATA);

  async copyPassword() {
    try {
      await navigator.clipboard.writeText(this.data.password);
      this.snack.open('Password copied to clipboard.', 'Close', { duration: 2500 });
    } catch {
      this.snack.open('Could not copy password. Please copy it manually.', 'Close', { duration: 3000 });
    }
  }

  close() {
    this.dialogRef?.close({ copied: true });
  }

}
