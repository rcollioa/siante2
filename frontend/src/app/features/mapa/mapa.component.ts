import { afterNextRender, Component, DestroyRef, ElementRef, inject, viewChild } from '@angular/core';
import { PanelCategoriasComponent } from '../categorias/panel-categorias.component';
import { BuscadorDireccionesComponent } from '../direcciones/buscador-direcciones.component';
import { MapaService } from './mapa.service';

@Component({
  selector: 'app-mapa',
  standalone: true,
  imports: [BuscadorDireccionesComponent, PanelCategoriasComponent],
  templateUrl: './mapa.component.html',
  styleUrl: './mapa.component.css'
})
export class MapaComponent {
  protected readonly mapaService = inject(MapaService);
  private readonly lienzo = viewChild.required<ElementRef<HTMLElement>>('lienzo');

  constructor() {
    afterNextRender(() => this.mapaService.montar(this.lienzo().nativeElement));
    inject(DestroyRef).onDestroy(() => this.mapaService.desmontar());
  }
}
