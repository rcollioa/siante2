import { ChangeDetectionStrategy, Component, computed, ElementRef, inject, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapasAbiertas } from '../../../application/capas-abiertas';
import { Catalogo } from '../../../application/catalogo';
import { CapaCatalogo, SeccionCatalogo, SECCIONES_CATALOGO } from '../../../domain/capa';

/** Filas que se dibujan a la vez: más se buscan con el buscador, como en un catálogo grande. */
export const MAXIMO_FILAS = 200;

/** El catálogo de capas de SIATE en un diálogo: pestañas "Todas", "Usuario" y "Capas formales", con buscador. Por
 * ahora solo consulta: marcar favoritas es una escritura y llega en una próxima versión. */
@Component({
  selector: 'app-catalogo-capas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './catalogo-capas.component.html',
  styleUrl: './catalogo-capas.component.css'
})
export class CatalogoCapasComponent {
  protected readonly catalogo = inject(Catalogo);
  private readonly capasAbiertas = inject(CapasAbiertas);
  private readonly router = inject(Router);
  private readonly menu = inject(MenuLateralService);
  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');
  private readonly pestanas = viewChild.required<ElementRef<HTMLElement>>('pestanas');

  protected readonly secciones: SeccionCatalogo[] = ['PUBLICAS', 'MIAS', 'FORMALES'];
  protected readonly nombres = SECCIONES_CATALOGO;
  protected readonly maximo = MAXIMO_FILAS;
  protected readonly visibles = computed(() => this.catalogo.capas().slice(0, MAXIMO_FILAS));

  abrir(seccion: SeccionCatalogo): void {
    this.catalogo.abrir(seccion);
    this.dialogo().nativeElement.showModal();
  }

  protected cerrar(): void {
    this.dialogo().nativeElement.close();
  }

  /** Cierra el catálogo y abre la capa sobre las demás en el mapa. */
  protected ver(capa: CapaCatalogo): void {
    this.cerrar();
    if (!this.router.url.startsWith('/mapa')) {
      void this.router.navigateByUrl('/mapa');
    }
    this.capasAbiertas.abrir(capa.id);
    this.menu.alElegir();
  }

  protected buscar(evento: Event): void {
    this.catalogo.buscar((evento.target as HTMLInputElement).value);
  }

  /** Teclado de pestañas (WAI-ARIA): flechas para la anterior o la siguiente, Inicio y Fin para los extremos. */
  protected moverPestana(evento: KeyboardEvent): void {
    const actual = this.secciones.indexOf(this.catalogo.seccion());
    const destino = {
      ArrowRight: (actual + 1) % this.secciones.length,
      ArrowLeft: (actual - 1 + this.secciones.length) % this.secciones.length,
      Home: 0,
      End: this.secciones.length - 1
    }[evento.key];
    if (destino === undefined) {
      return;
    }
    evento.preventDefault();
    this.catalogo.mostrar(this.secciones[destino]);
    this.pestanas().nativeElement.querySelectorAll<HTMLElement>('[role="tab"]')[destino]?.focus();
  }
}
