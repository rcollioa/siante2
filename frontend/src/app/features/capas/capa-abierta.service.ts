import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { MapaService } from '../mapa/mapa.service';
import { Capa, CapasApiService, ElementoCapa } from './capas-api.service';
import { aFeatures, ELEMENTO } from './elementos-openlayers';

/** La capa abierta en el mapa: una a la vez, como en SIATE, donde abrir una capa reemplazaba a la anterior. */
@Injectable({ providedIn: 'root' })
export class CapaAbiertaService {
  private readonly api = inject(CapasApiService);
  private readonly mapa = inject(MapaService);
  private pedido?: Subscription;

  readonly capa = signal<Capa | null>(null);
  /** ID de la capa que se está descargando; null si ninguna. */
  readonly abriendo = signal<number | null>(null);
  readonly error = signal('');
  /** Información del elemento en que el usuario hizo clic en el mapa. */
  readonly elemento = computed(() => (this.mapa.elementoSeleccionado()?.get(ELEMENTO) as ElementoCapa | undefined) ?? null);

  abrir(id: number): void {
    this.pedido?.unsubscribe();
    this.abriendo.set(id);
    this.error.set('');
    this.pedido = this.api.ver(id).subscribe({
      next: (capa) => {
        this.abriendo.set(null);
        this.capa.set(capa);
        this.mapa.mostrarElementos(capa.tipo === 'URL' ? [] : aFeatures(capa.elementos));
      },
      // Si falla, la capa que estaba abierta sigue en el mapa.
      error: (error: unknown) => {
        this.abriendo.set(null);
        this.error.set(
          error instanceof HttpErrorResponse && error.status === 404
            ? 'La capa ya no existe o no tiene permiso para verla.'
            : 'No se pudo abrir la capa. Intente nuevamente.'
        );
      }
    });
  }

  cerrar(): void {
    this.pedido?.unsubscribe();
    this.abriendo.set(null);
    this.error.set('');
    this.capa.set(null);
    this.mapa.quitarElementos();
  }
}
