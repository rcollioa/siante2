import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { CapasAbiertas } from '../../../application/capas-abiertas';

/** Sobre el mapa: la información del elemento elegido con un clic y su capa, o cómo elegir uno. */
@Component({
  selector: 'app-panel-capa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe],
  templateUrl: './panel-capa.component.html',
  styleUrl: './panel-capa.component.css'
})
export class PanelCapaComponent {
  protected readonly servicio = inject(CapasAbiertas);
  protected readonly elementosVisibles = computed(() =>
    this.servicio
      .abiertas()
      .filter(({ visible }) => visible)
      .reduce((total, { capa }) => total + capa.elementos.length, 0)
  );
  /** Capas por URL que todavía no se dibujan: WFS (las WMS y WMTS sí). */
  protected readonly hayPorUrl = computed(() =>
    this.servicio.abiertas().some(({ capa }) => capa.tipo === 'URL' && capa.servicio?.tipo !== 'WMS' && capa.servicio?.tipo !== 'WMTS')
  );
  /** Capas que se dibujan con imágenes de su servidor (WMS o WMTS), sin elementos que contar. */
  protected readonly hayWms = computed(() =>
    this.servicio.abiertas().some(({ capa }) => capa.servicio?.tipo === 'WMS' || capa.servicio?.tipo === 'WMTS')
  );
  /** Con solo capas WMS o WMTS que responden no hay nada que decir: el panel no ocupa el mapa. */
  protected readonly hayAviso = computed(
    () => this.servicio.sinRespuesta().length > 0 || this.elementosVisibles() > 0 || this.hayPorUrl() || !this.hayWms()
  );
}
