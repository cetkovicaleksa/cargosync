import {
  Component,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { NgIf } from '@angular/common';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-protected-image',
  standalone: true,
  imports: [NgIf],
  templateUrl: './protected-image.component.html'
})
export class ProtectedImageComponent implements OnChanges, OnDestroy {

  @Input() src!: string;
  @Input() alt = '';
  @Input() fallback = 'https://picsum.photos/200';

  imageUrl?: string;
  loading = false;
  error = false;
  readonly imageBaseUrl = environment.imageBaseUrl;

  private sub?: Subscription;

  constructor(private http: HttpClient) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['src'] && this.src) {
      this.loadImage();
    }
  }

  private loadImage() {
    this.cleanup();

    this.loading = true;
    this.error = false;

    if (this.isDirectImageUrl(this.src)) {
      this.imageUrl = this.src;
      this.loading = false;
      return;
    }

    this.sub = this.http.get(this.imageBaseUrl + this.src, {
      responseType: 'blob'
    }).subscribe({
      next: blob => {
        this.imageUrl = URL.createObjectURL(blob);
        this.loading = false;
      },
      error: (err) => {
        console.log(err);
        this.error = true;
        this.loading = false;
        this.imageUrl = this.fallback;
      }
    });
  }

  private isDirectImageUrl(src: string): boolean {
    return src.startsWith('blob:')
      || src.startsWith('data:')
      || src.startsWith('http://')
      || src.startsWith('https://');
  }

  private cleanup() {
    if (this.sub) {
      this.sub.unsubscribe();
    }

    if (this.imageUrl && this.imageUrl.startsWith('blob:')) {
      URL.revokeObjectURL(this.imageUrl);
    }

    this.imageUrl = undefined;
  }

  ngOnDestroy(): void {
    this.cleanup();
  }
}