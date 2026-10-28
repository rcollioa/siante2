import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapasAbiertas } from '../../../application/capas-abiertas';
import { MisCapas } from '../../../application/mis-capas';
import { ResumenCapa, VISIBILIDADES } from '../../../domain/capa';

/** Lista de "Mis capas" dentro del menú, como en SIATE: cada capa se abre sobre las demás en el mapa, y abierta se
 * puede ocultar o acercar. */
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
  protected readonly capasAbiertas = inject(CapasAbiertas);
  protected readonly visibilidades = VISIBILIDADES;
  protected readonly abiertasPorId = computed(
    () => new Map(this.capasAbiertas.abiertas().map((abierta) => [abierta.capa.id, abierta]))
  );

  constructor() {
    this.misCapas.cargar();
  }

  /** Elegir una capa abierta la cierra. Desde otra página (Ayuda), primero vuelve al mapa. */
  protected alternar(capa: ResumenCapa): void {
    if (this.abiertasPorId().has(capa.id)) {
      this.capasAbiertas.cerrar(capa.id);
      return;
    }
    this.irAlMapa();
    this.capasAbiertas.abrir(capa.id);
  }

  protected acercar(capa: ResumenCapa): void {
    this.irAlMapa();
    this.capasAbiertas.acercar(capa.id);
  }

  private irAlMapa(): void {
    if (!this.router.url.startsWith('/mapa')) {
      void this.router.navigateByUrl('/mapa');
    }
    this.menu.alElegir();
  }
}
