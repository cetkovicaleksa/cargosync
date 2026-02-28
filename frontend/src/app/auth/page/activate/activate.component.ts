import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-activate',
  standalone: true,
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './activate.component.html',
  styleUrl: './activate.component.scss',
})
export class ActivateComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  status: 'loading' | 'success' | 'error' = 'loading';
  message = '';

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.status = 'error';
      this.message = 'Invalid activation link.';
      return;
    }

    this.auth.activate(token).subscribe({
      next: (res) => {
        this.status = 'success';
        this.message = res.message;
      },
      error: (err) => {
        this.status = 'error';
        this.message = err?.error?.message ?? 'Activation failed. The link may have expired or already been used.';
      },
    });
  }
}
