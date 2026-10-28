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
