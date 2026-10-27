import { afterNextRender, Component, DestroyRef, ElementRef, inject, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AyudaApiService } from '../ayuda/ayuda-api.service';
import { PanelCapaComponent } from '../capas/panel-capa.component';
import { BuscadorDireccionesComponent } from '../direcciones/buscador-direcciones.component';
import { MapaService } from './mapa.service';

@Component({
  selector: 'app-mapa',
  standalone: true,
  imports: [RouterLink, BuscadorDireccionesComponent, PanelCapaComponent],
  templateUrl: './mapa.component.html',
  styleUrl: './mapa.component.css'
})
export class MapaComponent {
  protected readonly mapaService = inject(MapaService);
  private readonly lienzo = viewChild.required<ElementRef<HTMLElement>>('lienzo');

  // Si no se puede obtener el manual, el botón lleva a la página de Ayuda.
  protected readonly manual = toSignal(
    inject(AyudaApiService)
      .urlManual()
      .pipe(catchError(() => of(null))),
    { initialValue: null }
  );

  constructor() {
    afterNextRender(() => {
      this.mapaService.montar(this.lienzo().nativeElement);
      this.mapaService.ubicarUsuario();
    });
    inject(DestroyRef).onDestroy(() => this.mapaService.desmontar());
  }

  protected alternarCapaBase(): void {
    this.mapaService.usarCapaBase(this.mapaService.capaBase() === 'satelital' ? 'mapa' : 'satelital');
  }
}
