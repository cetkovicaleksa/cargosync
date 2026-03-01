import {Component, inject, OnInit} from '@angular/core';
import {AuthService} from '../../../auth/service/auth.service';
import { ProtectedImageComponent } from "../../util/protected-image.component";
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-home',
  imports: [ProtectedImageComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  authService = inject(AuthService)
  constructor() {
  }
  ngOnInit(): void {
  }
}
