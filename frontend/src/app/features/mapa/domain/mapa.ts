/** Capas base del mapa, las mismas de SIATE; solo una visible a la vez. */
export type CapaBase = 'mapa' | 'satelital';

/** Por qué no se muestra la posición del usuario. */
export type AvisoUbicacion = 'no-disponible' | 'sin-permiso';

/** Centro (EPSG:4326) y zoom del mapa. */
export interface ZonaVisible {
  latitud: number;
  longitud: number;
  zoom: number;
}

/** Geometría GeoJSON en EPSG:4326. */
export interface Geometria {
  type: string;
  coordinates: unknown[];
}

/** El estilo que guardó SIATE; un valor vacío o 0 usa el de la aplicación. */
export interface EstiloElemento {
  color: string;
  grosor: number;
  relleno: string;
  icono: string;
}

/** Lo que el mapa necesita para dibujar un elemento. Los círculos de SIATE son un punto con radio mayor que 0,
 * en unidades del mapa (EPSG:3857). */
export interface ElementoMapa {
  geometria: Geometria;
  radio: number;
  estilo: EstiloElemento;
}

/** Rectángulo en grados (EPSG:4326). */
export interface Extension {
  oeste: number;
  sur: number;
  este: number;
  norte: number;
}

/** Una capa de un servidor WMS: el mapa pide sus imágenes a `url` (el backend, que habla con el servidor). */
export interface CapaWms {
  url: string;
  capas: readonly string[];
  /** Para acercarse a la capa; null si el servidor no la informa. */
  extension: Extension | null;
}

/** El elemento en que el usuario hizo clic (el mismo objeto que se entregó) y la capa del mapa que lo muestra. */
export interface ElementoSeleccionado {
  capa: number;
  elemento: ElementoMapa;
}
