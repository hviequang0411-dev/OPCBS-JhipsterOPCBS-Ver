import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'jhi-footer',
  templateUrl: './footer.html',
  styleUrl: './footer.scss',
  imports: [RouterLink],
})
export default class Footer {
  readonly currentYear = new Date().getFullYear();
}
