import { afterNextRender, ChangeDetectionStrategy, Component, DestroyRef, ElementRef, inject, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { AyudaApiService } from '../../../../ayuda/ayuda-api.service';
import { BarraDibujoComponent } from '../../../../capas/adapter/in/ui/barra-dibujo.component';
import { PanelCapaComponent } from '../../../../capas/adapter/in/ui/panel-capa.component';
import { BuscadorDireccionesComponent } from '../../../../direcciones/buscador-direcciones.component';
import { MapaPort } from '../../../application/port/out/mapa.port';

@Component({
  selector: 'app-mapa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, BuscadorDireccionesComponent, PanelCapaComponent, BarraDibujoComponent],
  templateUrl: './mapa.component.html',
  styleUrl: './mapa.component.css'
})
export class MapaComponent {
  protected readonly mapa = inject(MapaPort);
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
      this.mapa.montar(this.lienzo().nativeElement);
      this.mapa.ubicarUsuario();
    });
    inject(DestroyRef).onDestroy(() => this.mapa.desmontar());
  }

  protected alternarCapaBase(): void {
    this.mapa.usarCapaBase(this.mapa.capaBase() === 'satelital' ? 'mapa' : 'satelital');
  }
}
