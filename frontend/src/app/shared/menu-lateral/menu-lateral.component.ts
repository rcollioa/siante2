import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { esPantallaChica } from '../pantalla/pantalla-chica';
import { SesionService } from '../sesion/sesion.service';
import { SECCIONES } from './secciones';

@Component({
  selector: 'app-menu-lateral',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './menu-lateral.component.html',
  styleUrl: './menu-lateral.component.css'
})
export class MenuLateralComponent {
  protected readonly sesion = inject(SesionService);
  protected readonly secciones = SECCIONES;
  // Como en SIANTE: empieza abierto en escritorio y cerrado en pantallas chicas.
  protected readonly abierto = signal(!esPantallaChica());

  protected alNavegar(): void {
    if (esPantallaChica()) {
      this.abierto.set(false);
    }
  }
}
