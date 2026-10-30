import { Signal } from '@angular/core';
import {
  AvisoUbicacion,
  CapaBase,
  CapaWms,
  CapaWmts,
  ElementoMapa,
  ElementoSeleccionado,
  PuntoWms,
  ZonaVisible
} from '../../../domain/mapa';

/** El mapa para el resto de la aplicación: instancia única, sin exponer la librería que lo dibuja. */
export abstract class MapaPort {
  abstract readonly capaBase: Signal<CapaBase>;
  /** Por qué no aparece la posición del usuario, para avisarle; null si no hay nada que avisar. */
  abstract readonly avisoUbicacion: Signal<AvisoUbicacion | null>;
  /** Esperando la posición que pidió el usuario con "Mi ubicación". */
  abstract readonly ubicando: Signal<boolean>;
  /** Las teselas de la capa base no cargan: sin salida a internet o servidor de mapas caído. */
  abstract readonly sinMapaBase: Signal<boolean>;
  /** Elemento en el que el usuario hizo clic; null si ninguno o si su capa se ocultó o se quitó. */
  abstract readonly elementoSeleccionado: Signal<ElementoSeleccionado | null>;
  /** Un clic fuera de los elementos, sobre capas WMS visibles: un punto por capa, la de más arriba primero. Vacío si
   * el clic fue sobre un elemento, si no hay capas WMS visibles o al deseleccionar. Cada clic entrega otro arreglo. */
  abstract readonly consultaWms: Signal<readonly PuntoWms[]>;
  /** Capas WMS cuyas imágenes no están llegando: el servidor de mapas no responde. */
  abstract readonly capasSinRespuesta: Signal<ReadonlySet<number>>;
  /** La regla de SIATE está activa: cada línea que se dibuja muestra su largo y los clics no eligen elementos. */
  abstract readonly midiendo: Signal<boolean>;

  abstract montar(elemento: HTMLElement): void;
  abstract desmontar(): void;
  abstract usarCapaBase(capa: CapaBase): void;
  abstract descartarAvisoMapaBase(): void;
  abstract descartarAvisoUbicacion(): void;

  /** Muestra los elementos de una capa, sobre las que ya están; si la capa ya estaba, reemplaza sus elementos. */
  abstract mostrarCapa(id: number, elementos: readonly ElementoMapa[]): void;
  /** Muestra una capa de un servidor WMS, sobre las que ya están; si ya estaba, la reemplaza. */
  abstract mostrarCapaWms(id: number, wms: CapaWms): void;
  /** Muestra las capas de un servidor WMTS como una sola capa abierta, sobre las que ya están; si ya estaba, la
   * reemplaza. */
  abstract mostrarCapaWmts(id: number, wmts: CapaWmts): void;
  abstract quitarCapa(id: number): void;
  abstract cambiarVisibilidad(id: number, visible: boolean): void;
  /** Acerca el mapa para ver todos los elementos de la capa. */
  abstract acercarA(id: number): void;
  abstract deseleccionar(): void;

  /** Activa la regla o, si estaba activa, la apaga y borra las mediciones, como en SIATE. */
  abstract alternarRegla(): void;
  /** Descarta la línea que se está midiendo; las ya terminadas quedan. */
  abstract cancelarMedicion(): void;

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
