import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, map, from } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ImageUploadService {
  private readonly http = inject(HttpClient);

  private readonly uploadBaseUrl = environment.nginxUploadUrl;
  private readonly imageBaseUrl = environment.imageBaseUrl;

  generateRandomImageName(file: File): string {
    const extension = this.getFileExtension(file.name);
    const id = typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    return extension ? `${id}.${extension}` : id;
  }

  uploadImage(file: File, imageName?: string): Observable<string> {
    // TODO: implement server-side endpoints for image upload
    return from(["https://picsum.photos/200"]);

    const finalName = imageName || this.generateRandomImageName(file);
    const uploadUrl = `${this.uploadBaseUrl}/${encodeURIComponent(finalName)}`;
    const headers = new HttpHeaders({
      'Content-Type': file.type || 'application/octet-stream',
    });

    return this.http.put(uploadUrl, file, { headers }).pipe(
      map(() => `${this.imageBaseUrl}/${finalName}`),
    );
  }

  private getFileExtension(fileName: string): string {
    const i = fileName.lastIndexOf('.');
    if (i < 0 || i === fileName.length - 1) return '';
    return fileName.slice(i + 1).toLowerCase();
  }
}
