import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, contentChild, inject, input, output, signal, TemplateRef } from '@angular/core';
import { Router } from '@angular/router';
import { MenuLateralService } from '../../../../../shared/menu-lateral/menu-lateral.service';
import { CapasAbiertas } from '../../../application/capas-abiertas';
import { EdicionElementos } from '../../../application/edicion-elementos';
import { ResumenCapa, sePuedeGestionar, VISIBILIDADES } from '../../../domain/capa';

/** Una lista de capas del menú, como en SIATE: cada capa se abre sobre las demás en el mapa, y abierta se puede
 * ocultar o acercar. La usan "Mis capas", "Favoritos" y "Capas formales". */
@Component({
  selector: 'app-lista-capas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [NgTemplateOutlet],
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
  /** Solo en "Mis capas": las opciones de una capa propia. Se despliegan bajo ella porque en los 340 px del menú no
   * caben junto al nombre. */
  protected readonly gestion = contentChild<TemplateRef<{ $implicit: ResumenCapa }>>('gestion');
  protected readonly desplegada = signal<number | null>(null);
  protected readonly sePuedeGestionar = sePuedeGestionar;

  private readonly router = inject(Router);
  private readonly menu = inject(MenuLateralService);
  protected readonly capasAbiertas = inject(CapasAbiertas);
  private readonly edicion = inject(EdicionElementos);
  protected readonly visibilidades = VISIBILIDADES;
  protected readonly abiertasPorId = computed(
    () => new Map(this.capasAbiertas.abiertas().map((abierta) => [abierta.capa.id, abierta]))
  );

  /** Elegir una capa abierta la cierra. Desde otra página (Ayuda), primero vuelve al mapa. */
  protected alternar(capa: ResumenCapa): void {
    if (!this.abiertasPorId().has(capa.id)) {
      this.abrir(capa);
    } else if (this.edicion.capa()?.id === capa.id) {
      // Si tiene cambios sin guardar, la barra de dibujo pregunta qué hacer: tiene que estar a la vista.
      this.irAlMapa();
      this.edicion.terminar(() => this.capasAbiertas.cerrar(capa.id));
    } else {
      this.capasAbiertas.cerrar(capa.id);
    }
  }

  abrir(capa: ResumenCapa): void {
    this.irAlMapa();
    this.capasAbiertas.abrir(capa.id);
  }

  protected alternarGestion(capa: ResumenCapa): void {
    this.desplegada.update((actual) => (actual === capa.id ? null : capa.id));
  }

  protected acercar(capa: ResumenCapa): void {
    this.irAlMapa();
    this.capasAbiertas.acercar(capa.id);
  }

  irAlMapa(): void {
    if (!this.router.url.startsWith('/mapa')) {
      void this.router.navigateByUrl('/mapa');
    }
    this.menu.alElegir();
  }
}
