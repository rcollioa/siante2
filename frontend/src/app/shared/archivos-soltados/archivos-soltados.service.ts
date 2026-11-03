import { Injectable, signal } from '@angular/core';

/** Lo arrastrado a la ventana, hasta que lo recibe la pantalla que lo importa: puede que aún no esté descargada. */
@Injectable({ providedIn: 'root' })
export class ArchivosSoltados {
  /** Si en este momento se está arrastrando un archivo que se podría soltar. */
  readonly arrastrando = signal(false);
  readonly pendientes = signal<readonly File[]>([]);
}
