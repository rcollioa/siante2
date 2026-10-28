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
  protected readonly hayPorUrl = computed(() => this.servicio.abiertas().some(({ capa }) => capa.tipo === 'URL'));
}
