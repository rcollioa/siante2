import { DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { CapaAbiertaService } from './capa-abierta.service';

/** Sobre el mapa: la capa abierta, para cerrarla, y la información del elemento elegido con un clic. */
@Component({
  selector: 'app-panel-capa',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './panel-capa.component.html',
  styleUrl: './panel-capa.component.css'
})
export class PanelCapaComponent {
  protected readonly servicio = inject(CapaAbiertaService);
}
