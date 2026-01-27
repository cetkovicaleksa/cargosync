import {Component} from '@angular/core';
import { MatAnchor } from '@angular/material/button';
import {RouterLink, RouterLinkActive} from '@angular/router';


const SECTIONS: {[key: string]: string} = {
  'home': 'Page One',
  'page-two': 'Page Two',
  'page-three': 'Page Three',
};
const SECTIONS_KEYS = Object.keys(SECTIONS);

@Component({
  selector: 'app-navbar',
  imports: [
    RouterLink,
    RouterLinkActive,
    MatAnchor
],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavBar {
  get sections() {
    return SECTIONS;
  }

  get sectionKeys() {
    return SECTIONS_KEYS;
  }
}