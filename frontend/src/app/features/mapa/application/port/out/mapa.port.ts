import { Signal } from '@angular/core';
import { AvisoUbicacion, CapaBase, ElementoMapa, ZonaVisible } from '../../../domain/mapa';

/** El mapa para el resto de la aplicación: instancia única, sin exponer la librería que lo dibuja. */
export abstract class MapaPort {
  abstract readonly capaBase: Signal<CapaBase>;
  /** Por qué no aparece la posición del usuario, para avisarle; null si no hay nada que avisar. */
  abstract readonly avisoUbicacion: Signal<AvisoUbicacion | null>;
  /** Esperando la posición que pidió el usuario con "Mi ubicación". */
  abstract readonly ubicando: Signal<boolean>;
  /** Las teselas de la capa base no cargan: sin salida a internet o servidor de mapas caído. */
  abstract readonly sinMapaBase: Signal<boolean>;
  /** Elemento mostrado en el que el usuario hizo clic (el mismo objeto que se entregó); null si ninguno. */
  abstract readonly elementoSeleccionado: Signal<ElementoMapa | null>;

  abstract montar(elemento: HTMLElement): void;
  abstract desmontar(): void;
  abstract usarCapaBase(capa: CapaBase): void;
  abstract descartarAvisoMapaBase(): void;
  abstract descartarAvisoUbicacion(): void;

  /** Muestra estos elementos en lugar de los anteriores y acerca el mapa para verlos todos. */
  abstract mostrarElementos(elementos: readonly ElementoMapa[]): void;
  abstract quitarElementos(): void;

  /** Centra el mapa en el punto (EPSG:4326) y lo marca. */
  abstract irA(longitud: number, latitud: number): void;
  abstract quitarMarcador(): void;
  /** Lo que se está mirando: las sugerencias de direcciones cercanas aparecen primero. */
  abstract zonaVisible(): ZonaVisible;

  /** Al abrir el mapa: lo centra en la posición del usuario y la marca. */
  abstract ubicarUsuario(): void;
  /** Botón "Mi ubicación": vuelve a la posición ya conocida o la pide de nuevo. */
  abstract irAMiUbicacion(): void;
}
