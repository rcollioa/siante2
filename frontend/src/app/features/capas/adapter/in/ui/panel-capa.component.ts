import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CapaAbierta } from '../../../application/capa-abierta';

/** Sobre el mapa: la capa abierta, para cerrarla, y la información del elemento elegido con un clic. */
@Component({
  selector: 'app-panel-capa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DecimalPipe],
  templateUrl: './panel-capa.component.html',
  styleUrl: './panel-capa.component.css'
})
export class PanelCapaComponent {
  protected readonly servicio = inject(CapaAbierta);
}
