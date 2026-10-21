import { DestroyRef, Injectable, Signal, effect, inject, signal, untracked } from '@angular/core';

/**
 * Estado de la pantalla de carga global (`PantallaCargaComponent`: logo + spinner + mensaje sobre un
 * fondo translúcido, mientras hay peticiones en curso). `pantallaCargaInterceptor` la muestra/oculta
 * solo alrededor de cualquier request a la API — no hace falta que las páginas la conozcan.
 *
 * El contador (no un simple booleano) es lo que evita que, con varias peticiones en paralelo (p. ej.
 * un `forkJoin`), la primera en terminar oculte la pantalla mientras las demás siguen en curso.
 */
@Injectable({ providedIn: 'root' })
export class PantallaCargaService {
  private readonly _pendientes = signal(0);
  private readonly _mensaje = signal('Cargando');
  private readonly _visible = signal(false);

  readonly visible = this._visible.asReadonly();
  readonly mensaje = this._mensaje.asReadonly();

  mostrar(mensaje = 'Cargando'): void {
    this._mensaje.set(mensaje);
    this._pendientes.update(valor => valor + 1);
    this._visible.set(true);
  }

  ocultar(): void {
    const pendientes = Math.max(0, this._pendientes() - 1);
    this._pendientes.set(pendientes);
    if (pendientes === 0) {
      this._visible.set(false);
    }
  }

  /** Muestra la pantalla y devuelve la función que la libera. Liberar más de una vez no descuenta de
   * más, así que quien la llama puede liberarla tanto al terminar como al destruirse sin cuidar el orden. */
  bloquear(mensaje = 'Cargando'): () => void {
    this.mostrar(mensaje);
    let liberada = false;
    return () => {
      if (!liberada) {
        liberada = true;
        this.ocultar();
      }
    };
  }
}

/** Tiempo mínimo visible de la pantalla de carga de una página. */
const DURACION_MINIMA_MS = 500;

/**
 * Pantalla de carga global mientras una página espera sus datos. Pensada para páginas cuyas consultas
 * no la muestran por sí solas (`sinPantallaDeCarga()`, para que un cambio de filtro no bloquee toda la
 * vista): esto la muestra solo al entrar a la página.
 *
 * Se llama en el contexto de inyección del componente (inicializador de campo). `listo` debe pasar a
 * true cuando la página tiene sus datos o cuando su consulta falló: si no, la pantalla no se cerraría.
 * También se libera al destruirse el componente. Devuelve la función que la muestra (`iniciar`), que
 * se llama cada vez que se entra a la página.
 *
 * Se muestra siempre, aunque los datos ya estén en caché, y como mínimo `DURACION_MINIMA_MS`: si no,
 * con datos al instante se abriría y cerraría sin alcanzar a verse.
 */
export function pantallaDeCargaDeCapa(listo: Signal<boolean>): () => void {
  const pantallaCarga = inject(PantallaCargaService);
  const esperando = signal(false);
  const minimoCumplido = signal(false);
  let liberar: (() => void) | null = null;
  let temporizador: ReturnType<typeof setTimeout> | undefined;
  const terminar = (): void => {
    clearTimeout(temporizador);
    liberar?.();
    liberar = null;
    esperando.set(false);
  };
  effect(() => {
    if (esperando() && minimoCumplido() && listo()) {
      untracked(terminar);
    }
  });
  inject(DestroyRef).onDestroy(terminar);
  return () => {
    clearTimeout(temporizador);
    liberar ??= pantallaCarga.bloquear();
    minimoCumplido.set(false);
    esperando.set(true);
    temporizador = setTimeout(() => minimoCumplido.set(true), DURACION_MINIMA_MS);
  };
}
