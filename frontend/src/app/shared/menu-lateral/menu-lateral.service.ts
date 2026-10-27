import { Injectable, signal } from '@angular/core';
import { esPantallaChica } from '../pantalla/pantalla-chica';

/** Estado del menú lateral, compartido con lo que se despliega dentro de él (como "Mis capas"). */
@Injectable({ providedIn: 'root' })
export class MenuLateralService {
  // Como en SIATE: empieza abierto en escritorio y cerrado en pantallas chicas.
  readonly abierto = signal(!esPantallaChica());

  /** En pantallas chicas el menú tapa el mapa: se cierra al elegir algo para que se vea el resultado. */
  alElegir(): void {
    if (esPantallaChica()) {
      this.abierto.set(false);
    }
  }
}
