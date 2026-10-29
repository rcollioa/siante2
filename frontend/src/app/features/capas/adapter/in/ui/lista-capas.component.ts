import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { Router } from '@angular/router';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapasAbiertas } from '../../../application/capas-abiertas';
import { ResumenCapa, VISIBILIDADES } from '../../../domain/capa';

/** Una lista de capas del menú, como en SIATE: cada capa se abre sobre las demás en el mapa, y abierta se puede
 * ocultar o acercar. La usan "Mis capas", "Favoritos" y "Capas formales". */
@Component({
  selector: 'app-lista-capas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './lista-capas.component.html',
  styleUrl: './lista-capas.component.css'
})
export class ListaCapasComponent {
  readonly capas = input.required<readonly ResumenCapa[]>();
  readonly estado = input.required<'cargando' | 'listo' | 'error'>();
  /** Qué se está cargando, para los avisos: "sus capas", "sus favoritos"… */
  readonly descripcion = input.required<string>();
  readonly vacia = input.required<string>();
  readonly reintentar = output();

  private readonly router = inject(Router);
  private readonly menu = inject(MenuLateralService);
  protected readonly capasAbiertas = inject(CapasAbiertas);
  protected readonly visibilidades = VISIBILIDADES;
  protected readonly abiertasPorId = computed(
    () => new Map(this.capasAbiertas.abiertas().map((abierta) => [abierta.capa.id, abierta]))
  );

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
