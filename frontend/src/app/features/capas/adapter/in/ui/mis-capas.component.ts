import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapaAbierta } from '../../../application/capa-abierta';
import { MisCapas } from '../../../application/mis-capas';
import { ResumenCapa, VISIBILIDADES } from '../../../domain/capa';

/** Lista de "Mis capas" dentro del menú, como en SIATE: al elegir una se abre en el mapa. */
@Component({
  selector: 'app-mis-capas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mis-capas.component.html',
  styleUrl: './mis-capas.component.css'
})
export class MisCapasComponent {
  private readonly router = inject(Router);
  private readonly menu = inject(MenuLateralService);
  protected readonly misCapas = inject(MisCapas);
  protected readonly capaAbierta = inject(CapaAbierta);
  protected readonly visibilidades = VISIBILIDADES;

  constructor() {
    this.misCapas.cargar();
  }

  /** Elegir la capa abierta la cierra. Desde otra página (Ayuda), primero vuelve al mapa. */
  protected alternar(capa: ResumenCapa): void {
    if (this.capaAbierta.capa()?.id === capa.id) {
      this.capaAbierta.cerrar();
      return;
    }
    if (!this.router.url.startsWith('/mapa')) {
      void this.router.navigateByUrl('/mapa');
    }
    this.capaAbierta.abrir(capa.id);
    this.menu.alElegir();
  }
}
