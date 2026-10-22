import { DestroyRef, Injectable, Signal, effect, inject, signal, untracked } from '@angular/core';

/** Estado de la pantalla de carga global. Usa un contador para que, con peticiones en paralelo,
 * la primera en terminar no la oculte. */
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

/** Pantalla de carga al entrar a la página, hasta que `listo` (datos o error) sea true.
 * Llamar en el contexto de inyección; devuelve `iniciar`, para cada entrada a la página. */
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
