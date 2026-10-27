import { Component, DOCUMENT, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
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
  private readonly router = inject(Router);
  private readonly ventana = inject(DOCUMENT).defaultView;

  protected alNavegar(): void {
    if (esPantallaChica()) {
      this.abierto.set(false);
    }
  }

  /** Como SIANTE, cierra la ventana. El navegador lo impide si no la abrió un script o ya tiene historial;
   * entonces queda a la vista la página de salida. No toca la sesión de WebSEAL, que es de otro sistema. */
  protected salir(): void {
    this.sesion.finalizar();
    this.router.navigateByUrl('/salida').then(() => this.ventana?.close());
  }
}
