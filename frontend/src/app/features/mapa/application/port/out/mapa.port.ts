import { Signal } from '@angular/core';
import {
  AvisoUbicacion,
  CapaBase,
  CapaWms,
  CapaWmts,
  ElementoLeido,
  ElementoMapa,
  ElementoSeleccionado,
  HerramientaDibujo,
  OyenteEdicion,
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

  /** Permite editar una capa vectorial ya mostrada, una sola a la vez: lo seleccionado en ella se mueve y cambia de
   * forma, y las herramientas dibujan en ella. */
  abstract editarCapa(id: number, oyente: OyenteEdicion): void;
  abstract terminarEdicion(): void;
  /** Con qué dibuja cada clic en la capa en edición, sin seleccionar; null vuelve a seleccionar. */
  abstract usarHerramienta(herramienta: HerramientaDibujo | null): void;
  /** Muestra un elemento nuevo y lo deja seleccionado. */
  abstract agregarElemento(capa: number, elemento: ElementoMapa): void;
  /** Cambia los datos o el estilo de un elemento mostrado; su forma es la que ya tiene en el mapa. */
  abstract reemplazarElemento(capa: number, anterior: ElementoMapa, nuevo: ElementoMapa): void;
  abstract quitarElemento(capa: number, elemento: ElementoMapa): void;

  /** Los elementos de un KML, sin mostrarlos; vacío si no se puede leer. Quien lee KML es la librería del mapa. */
  abstract leerKml(texto: string): Promise<ElementoLeido[]>;

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
