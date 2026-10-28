import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { Capa, CapaNoEncontrada, ElementoCapa } from '../domain/capa';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** La capa abierta en el mapa: una a la vez, como en SIATE, donde abrir una capa reemplazaba a la anterior. */
@Injectable({ providedIn: 'root' })
export class CapaAbierta {
  private readonly repositorio = inject(CapasRepositorio);
  private readonly mapa = inject(MapaPort);
  private pedido?: Subscription;
  private readonly capaActual = signal<Capa | null>(null);
  private readonly idAbriendo = signal<number | null>(null);
  private readonly mensajeError = signal('');

  readonly capa = this.capaActual.asReadonly();
  /** ID de la capa que se está descargando; null si ninguna. */
  readonly abriendo = this.idAbriendo.asReadonly();
  readonly error = this.mensajeError.asReadonly();
  /** Información del elemento en que el usuario hizo clic. Solo la capa abierta muestra elementos en el mapa,
   * así que lo seleccionado es uno de los suyos. */
  readonly elemento = computed(() => this.mapa.elementoSeleccionado() as ElementoCapa | null);

  abrir(id: number): void {
    this.pedido?.unsubscribe();
    this.idAbriendo.set(id);
    this.mensajeError.set('');
    this.pedido = this.repositorio.ver(id).subscribe({
      next: (capa) => {
        this.idAbriendo.set(null);
        this.capaActual.set(capa);
        this.mapa.mostrarElementos(capa.tipo === 'URL' ? [] : capa.elementos);
      },
      // Si falla, la capa que estaba abierta sigue en el mapa.
      error: (error: unknown) => {
        this.idAbriendo.set(null);
        this.mensajeError.set(
          error instanceof CapaNoEncontrada
            ? 'La capa ya no existe o no tiene permiso para verla.'
            : 'No se pudo abrir la capa. Intente nuevamente.'
        );
      }
    });
  }

  cerrar(): void {
    this.pedido?.unsubscribe();
    this.idAbriendo.set(null);
    this.mensajeError.set('');
    this.capaActual.set(null);
    this.mapa.quitarElementos();
  }

  descartarError(): void {
    this.mensajeError.set('');
  }
}
