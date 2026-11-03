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

/** Un nivel de zoom de una grilla WMTS: resolución en unidades del mapa por píxel, esquina superior izquierda,
 * tamaño de cada mosaico y cuántos hay. */
export interface MatrizMosaicos {
  id: string;
  resolucion: number;
  origenX: number;
  origenY: number;
  anchoMosaico: number;
  altoMosaico: number;
  columnas: number;
  filas: number;
}

/** La grilla de mosaicos de un servidor WMTS, de menos a más zoom; `crs`: EPSG:3857 o EPSG:4326. */
export interface GrillaWmts {
  crs: string;
  matrices: readonly MatrizMosaicos[];
}

/** Las capas de un servidor WMTS: el mapa pide cada mosaico a `url` (el backend) seguido de la capa, la matriz, la
 * fila y la columna. */
export interface CapaWmts {
  url: string;
  capas: readonly string[];
  /** Para acercarse a la capa; null si el servidor no la informa. */
  extension: Extension | null;
  grilla: GrillaWmts;
}

/** Un clic sobre una capa WMS: la imagen del servidor en que se hizo, como la pidió el mapa, y el píxel dentro de
 * ella (`i` de izquierda a derecha, `j` de arriba abajo). Es lo que el servidor necesita para decir qué hay ahí. */
export interface PuntoWms {
  capa: number;
  capas: readonly string[];
  bbox: string;
  ancho: number;
  alto: number;
  crs: string;
  version: string;
  i: number;
  j: number;
}

/** El elemento en que el usuario hizo clic (el mismo objeto que se entregó) y la capa del mapa que lo muestra. */
export interface ElementoSeleccionado {
  capa: number;
  elemento: ElementoMapa;
}

/** Lo que se dibuja en una capa, como en SIATE. `Box` es un cuadro: se guarda como polígono. */
export type HerramientaDibujo = 'Point' | 'LineString' | 'Box' | 'Polygon' | 'Circle';

/** La forma de un elemento en el mapa, como en `ElementoMapa`. */
export type FormaElemento = Pick<ElementoMapa, 'geometria' | 'radio'>;

/** Lo que el usuario hace sobre la capa en edición. El mapa no decide qué elemento resulta: lo muestra quien escucha. */
export interface OyenteEdicion {
  alDibujar(forma: FormaElemento): void;
  /** Lo movió o cambió sus vértices: el mapa ya lo muestra con su forma nueva. */
  alCambiarForma(elemento: ElementoMapa, forma: FormaElemento): void;
}

// Los de SIATE para lo que no definió su estilo (getDefaultStyle en su map.service).
export const COLOR_POR_DEFECTO = '#71aecb';
export const RELLENO_POR_DEFECTO = 'rgba(230, 248, 253, 0.35)';
export const GROSOR_POR_DEFECTO = 2;
