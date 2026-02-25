import {Component, inject} from '@angular/core';
import {AuthService} from '../../../auth/service/auth.service';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  authService = inject(AuthService)
  constructor() {
  }
}
