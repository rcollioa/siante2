import { computed, inject, Injectable, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import { Capa, CapaNoEncontrada, ElementoCapa } from '../domain/capa';
import { CapasRepositorio } from './port/out/capas.repositorio';

/** Una capa abierta en el mapa y si se está mostrando. */
export interface CapaEnMapa {
  capa: Capa;
  visible: boolean;
}

/** El elemento en que el usuario hizo clic y la capa abierta a la que pertenece. */
export interface ElementoElegido {
  capa: Capa;
  elemento: ElementoCapa;
}

/** Las capas abiertas en el mapa, sobrepuestas como en SIATE: cada una se puede ocultar, acercar o cerrar. */
@Injectable({ providedIn: 'root' })
export class CapasAbiertas {
  private readonly repositorio = inject(CapasRepositorio);
  private readonly mapa = inject(MapaPort);
  private readonly pedidos = new Map<number, Subscription>();
  private readonly lista = signal<readonly CapaEnMapa[]>([]);
  private readonly idsAbriendo = signal<ReadonlySet<number>>(new Set());
  private readonly mensajeError = signal('');

  /** En el orden en que se abrieron: la última queda encima en el mapa. */
  readonly abiertas = this.lista.asReadonly();
  /** IDs de las capas que se están descargando. */
  readonly abriendo = this.idsAbriendo.asReadonly();
  readonly error = this.mensajeError.asReadonly();
  /** Los elementos en el mapa son solo los de las capas abiertas, así que lo seleccionado es de una de ellas. */
  readonly elegido = computed<ElementoElegido | null>(() => {
    const seleccion = this.mapa.elementoSeleccionado();
    const abierta = seleccion && this.lista().find(({ capa }) => capa.id === seleccion.capa);
    return abierta ? { capa: abierta.capa, elemento: seleccion.elemento as ElementoCapa } : null;
  });

  estaAbierta(id: number): boolean {
    return this.lista().some(({ capa }) => capa.id === id);
  }

  /** Abre la capa sobre las demás y acerca el mapa a ella. Si ya estaba abierta, la muestra y la acerca. */
  abrir(id: number): void {
    this.mensajeError.set('');
    if (this.estaAbierta(id)) {
      this.cambiarVisibilidad(id, true);
      this.mapa.acercarA(id);
      return;
    }
    if (this.pedidos.has(id)) {
      return;
    }
    this.idsAbriendo.update((ids) => new Set(ids).add(id));
    this.pedidos.set(
      id,
      this.repositorio.ver(id).subscribe({
        next: (capa) => {
          this.terminarPedido(id);
          this.lista.update((abiertas) => [...abiertas, { capa, visible: true }]);
          this.mapa.mostrarCapa(id, capa.tipo === 'URL' ? [] : capa.elementos);
          this.mapa.acercarA(id);
          // Como el marcador del buscador en SIATE: desaparece al elegir otra capa.
          this.mapa.quitarMarcador();
        },
        // Si falla, las capas que ya estaban abiertas siguen en el mapa.
        error: (error: unknown) => {
          this.terminarPedido(id);
          this.mensajeError.set(
            error instanceof CapaNoEncontrada
              ? 'La capa ya no existe o no tiene permiso para verla.'
              : 'No se pudo abrir la capa. Intente nuevamente.'
          );
        }
      })
    );
  }

  cerrar(id: number): void {
    this.pedidos.get(id)?.unsubscribe();
    this.terminarPedido(id);
    this.lista.update((abiertas) => abiertas.filter(({ capa }) => capa.id !== id));
    this.mapa.quitarCapa(id);
  }

  alternarVisibilidad(id: number): void {
    const abierta = this.lista().find(({ capa }) => capa.id === id);
    if (abierta) {
      this.cambiarVisibilidad(id, !abierta.visible);
    }
  }

  /** Acercarse a una capa oculta no serviría de nada: también la muestra. */
  acercar(id: number): void {
    if (this.estaAbierta(id)) {
      this.cambiarVisibilidad(id, true);
      this.mapa.acercarA(id);
    }
  }

  deseleccionar(): void {
    this.mapa.deseleccionar();
  }

  descartarError(): void {
    this.mensajeError.set('');
  }

  private cambiarVisibilidad(id: number, visible: boolean): void {
    this.lista.update((abiertas) => abiertas.map((abierta) => (abierta.capa.id === id ? { ...abierta, visible } : abierta)));
    this.mapa.cambiarVisibilidad(id, visible);
  }

  private terminarPedido(id: number): void {
    this.pedidos.delete(id);
    this.idsAbriendo.update((ids) => {
      const restantes = new Set(ids);
      restantes.delete(id);
      return restantes;
    });
  }
}
