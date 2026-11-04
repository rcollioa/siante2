import { ElementoLeido, Geometria, sinAltura } from '../../mapa/domain/mapa';
import { elementoNuevo } from './cambios-elementos';
import { ElementoCapa, MAXIMO_METADATOS, MAXIMO_NOMBRE_ELEMENTO, MAXIMO_TEXTO_ELEMENTO } from './capa';

/** De qué tipo de archivo viene una capa importada. */
export type OrigenArchivo = 'KML' | 'GEOJSON' | 'CSV' | 'XLSX';

/** El aviso de SIATE dice 30 MB. */
export const MAXIMO_BYTES_ARCHIVO = 30 * 1024 * 1024;
/** El backend exige lo mismo. */
export const MAXIMO_ELEMENTOS_IMPORTADOS = 10000;
/** Lo que SIATE envía en una sola petición. */
const MAXIMO_CARACTERES_IMPORTADOS = 20_000_000;

const EXTENSIONES_GEOGRAFICAS = ['.kml', '.json', '.geojson'];
const EXTENSIONES_TABLA = ['.csv', '.xlsx'];
export const EXTENSIONES_ARCHIVO = [...EXTENSIONES_GEOGRAFICAS, ...EXTENSIONES_TABLA];

/** El archivo no se puede importar; el mensaje es para el usuario. */
export class ArchivoInvalido extends Error {
  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'ArchivoInvalido';
  }
}

/** Un archivo leído y revisado, listo para crear la capa. */
export interface ArchivoLeido {
  origen: OrigenArchivo;
  elementos: ElementoCapa[];
  /** Los que no tenían una geometría válida. */
  omitidos: number;
  /** SIATE crea un grupo con un KML que tiene carpetas; los grupos todavía no están migrados. */
  enCarpetas: boolean;
}

export function origenDe(nombreArchivo: string): OrigenArchivo | null {
  const nombre = nombreArchivo.toLowerCase();
  if (!EXTENSIONES_GEOGRAFICAS.some((extension) => nombre.endsWith(extension))) {
    return null;
  }
  return nombre.endsWith('.kml') ? 'KML' : 'GEOJSON';
}

/** Un Excel o un CSV: antes de crear la capa hay que elegir en qué columnas está la ubicación. */
export function esTabla(nombreArchivo: string): boolean {
  const nombre = nombreArchivo.toLowerCase();
  return EXTENSIONES_TABLA.some((extension) => nombre.endsWith(extension));
}

export function nombreSugerido(nombreArchivo: string, maximo: number): string {
  return nombreArchivo.replace(/\.[^.]+$/, '').trim().slice(0, maximo);
}

/** UTF-8, como exige GeoJSON; si no lo es, lo más común en un archivo antiguo en español. */
export function decodificar(contenido: ArrayBuffer): string {
  try {
    return new TextDecoder('utf-8', { fatal: true }).decode(contenido);
  } catch {
    return new TextDecoder('windows-1252').decode(contenido);
  }
}

export function tieneCarpetas(kml: string): boolean {
  return (kml.match(/<Folder[\s>]/g)?.length ?? 0) > 1;
}

type Objeto = Record<string, unknown>;

/** Una colección, un elemento o una geometría suelta. SIATE solo aceptaba colecciones. */
export function leerGeoJson(texto: string): ElementoLeido[] {
  let datos: unknown;
  try {
    datos = JSON.parse(texto);
  } catch {
    throw new ArchivoInvalido('El archivo no tiene un formato válido.');
  }
  if (!esObjeto(datos) && !Array.isArray(datos)) {
    throw new ArchivoInvalido('El archivo no tiene un formato válido.');
  }
  exigirWgs84(datos);
  return featuresDe(datos).flatMap(elementosDe);
}

/** Sin este aviso, un archivo en UTM o en metros terminaría como "coordenadas no válidas", sin decir por qué. */
function exigirWgs84(datos: unknown): void {
  const crs = esObjeto(datos) && esObjeto(datos['crs']) && esObjeto(datos['crs']['properties']) ? datos['crs']['properties']['name'] : '';
  if (typeof crs === 'string' && crs !== '' && !/CRS84|4326/i.test(crs)) {
    throw new ArchivoInvalido(`El archivo usa el sistema de coordenadas ${crs}. Debe estar en WGS84 (EPSG:4326).`);
  }
}

function featuresDe(datos: unknown): unknown[] {
  if (Array.isArray(datos)) {
    return datos;
  }
  if (esObjeto(datos) && Array.isArray(datos['features'])) {
    return datos['features'];
  }
  if (esObjeto(datos) && datos['type'] === 'Feature') {
    return [datos];
  }
  if (esObjeto(datos) && (Array.isArray(datos['coordinates']) || Array.isArray(datos['geometries']))) {
    return [{ type: 'Feature', geometry: datos }];
  }
  throw new ArchivoInvalido('El archivo no tiene un formato válido.');
}

/** Un elemento sin geometría se entrega igual, vacío: `revisar` lo cuenta entre los omitidos. */
function elementosDe(feature: unknown): ElementoLeido[] {
  const elemento = esObjeto(feature) ? feature : {};
  const datos = datosDe(elemento);
  return formasDe(elemento['geometry']).map((forma) => ({ ...forma, ...datos }));
}

function formasDe(geometria: unknown): Pick<ElementoLeido, 'geometria' | 'radio'>[] {
  if (esObjeto(geometria) && Array.isArray(geometria['geometries'])) {
    return geometria['geometries'].flatMap(formasDe);
  }
  if (esObjeto(geometria) && geometria['type'] === 'Circle' && Array.isArray(geometria['center'])) {
    // Como guarda SIATE los círculos: al exportarlos y volver a importarlos siguen siendo círculos.
    return [{ geometria: { type: 'Point', coordinates: geometria['center'] }, radio: Number(geometria['radius']) || 0 }];
  }
  return esObjeto(geometria) && typeof geometria['type'] === 'string' && Array.isArray(geometria['coordinates'])
    ? [{ geometria: { type: geometria['type'], coordinates: geometria['coordinates'] }, radio: 0 }]
    : [{ geometria: { type: '', coordinates: [] }, radio: 0 }];
}

/** Lo que SIATE exporta trae el estilo en `properties` y los datos en `metadata`. En otro GeoJSON, `properties` son los
 * datos, y SIATE no los mostraba. Las de estilo son las de la convención de geojson.io (`stroke`, `fill`…). */
function datosDe(feature: Objeto): Pick<ElementoLeido, 'nombre' | 'descripcion' | 'metadatos' | 'estilo'> {
  const propiedades = esObjeto(feature['properties']) ? feature['properties'] : {};
  if (esObjeto(feature['metadata'])) {
    const metadata = feature['metadata'];
    return {
      nombre: textoDe(metadata['name']),
      descripcion: textoDe(metadata['description']),
      metadatos: (Array.isArray(metadata['metadata']) ? metadata['metadata'] : [])
        .filter(esObjeto)
        .map((dato) => ({ nombre: textoDe(dato['name']), valor: textoDe(dato['description']) })),
      estilo: {
        color: colorDe(propiedades['color']),
        grosor: grosorDe(propiedades['width']),
        relleno: colorDe(propiedades['fill']),
        icono: ''
      }
    };
  }
  const claves = Object.keys(propiedades);
  const nombre = claves.find((clave) => /^(name|nombre)$/i.test(clave));
  const descripcion = claves.find((clave) => /^(description|descripcion|descripción)$/i.test(clave));
  return {
    nombre: nombre ? textoDe(propiedades[nombre]) : '',
    descripcion: descripcion ? textoDe(propiedades[descripcion]) : '',
    metadatos: claves
      .filter((clave) => clave !== nombre && clave !== descripcion && !ESTILO_GEOJSON.has(clave))
      .map((clave) => ({ nombre: clave, valor: textoDe(propiedades[clave]) })),
    estilo: {
      color: colorDe(propiedades['stroke']),
      grosor: grosorDe(propiedades['stroke-width']),
      relleno: rellenoDe(propiedades['fill'], propiedades['fill-opacity']),
      icono: ''
    }
  };
}

const ESTILO_GEOJSON = new Set([
  'stroke',
  'stroke-width',
  'stroke-opacity',
  'fill',
  'fill-opacity',
  'marker-color',
  'marker-size',
  'marker-symbol'
]);

function rellenoDe(color: unknown, opacidad: unknown): string {
  const relleno = colorDe(color);
  const hexadecimal = /^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec(relleno);
  if (!hexadecimal || typeof opacidad !== 'number') {
    return relleno;
  }
  const [rojo, verde, azul] = hexadecimal.slice(1).map((parte) => parseInt(parte, 16));
  return `rgba(${rojo},${verde},${azul},${opacidad})`;
}

/** Un dato que se llame `color` en otro archivo puede ser "rojo": solo se usa lo que el mapa sabe dibujar. */
function colorDe(valor: unknown): string {
  if (typeof valor === 'string' && /^(#[0-9a-f]{3,8}|rgba?\([0-9., %/]{5,40}\))$/i.test(valor.trim())) {
    return valor.trim();
  }
  if (Array.isArray(valor) && (valor.length === 3 || valor.length === 4) && valor.every((parte) => typeof parte === 'number')) {
    return `${valor.length === 4 ? 'rgba' : 'rgb'}(${valor.join(', ')})`;
  }
  return '';
}

function grosorDe(valor: unknown): number {
  return typeof valor === 'number' && valor > 0 && valor <= 100 ? valor : 0;
}

function textoDe(valor: unknown): string {
  if (valor === null || valor === undefined) {
    return '';
  }
  return typeof valor === 'object' ? JSON.stringify(valor) : String(valor);
}

function esObjeto(valor: unknown): valor is Objeto {
  return typeof valor === 'object' && valor !== null && !Array.isArray(valor);
}

/** Deja listos los elementos de cualquier origen: los que no se pueden guardar se omiten, porque uno solo haría rechazar
 * todo el archivo. Falla si no queda nada o si son demasiados. */
export function revisar(leidos: readonly ElementoLeido[], origen: OrigenArchivo, enCarpetas: boolean): ArchivoLeido {
  const elementos = leidos.flatMap((leido, indice) => {
    const geometria = geometriaValida(leido.geometria);
    return geometria && (leido.radio === 0 || (leido.radio > 0 && geometria.type === 'Point'))
      ? [listo(leido, geometria, -(indice + 1))]
      : [];
  });
  if (elementos.length === 0) {
    throw new ArchivoInvalido(
      leidos.length === 0
        ? 'El archivo no tiene geometrías.'
        : 'Las coordenadas del archivo no son válidas: deben ser longitud y latitud en grados (WGS84, EPSG:4326).'
    );
  }
  if (elementos.length > MAXIMO_ELEMENTOS_IMPORTADOS) {
    throw new ArchivoInvalido(`El archivo tiene ${elementos.length} elementos, y el máximo es ${MAXIMO_ELEMENTOS_IMPORTADOS}.`);
  }
  if (JSON.stringify(elementos).length > MAXIMO_CARACTERES_IMPORTADOS) {
    throw new ArchivoInvalido('El contenido del archivo es demasiado grande para importarlo.');
  }
  return { origen, elementos, omitidos: leidos.length - elementos.length, enCarpetas };
}

function listo(leido: ElementoLeido, geometria: Geometria, id: number): ElementoCapa {
  return {
    ...elementoNuevo(id, { geometria, radio: leido.radio }),
    nombre: leido.nombre.trim().slice(0, MAXIMO_NOMBRE_ELEMENTO),
    descripcion: leido.descripcion.trim().slice(0, MAXIMO_TEXTO_ELEMENTO),
    estilo: leido.estilo,
    metadatos: leido.metadatos
      .filter(({ nombre }) => nombre.trim() !== '')
      .slice(0, MAXIMO_METADATOS)
      .map(({ nombre, valor }) => ({
        nombre: nombre.trim().slice(0, MAXIMO_NOMBRE_ELEMENTO),
        valor: valor.trim().slice(0, MAXIMO_TEXTO_ELEMENTO)
      }))
  };
}

/** En dos dimensiones y con los polígonos cerrados, o null si Oracle Spatial no la aceptaría. */
function geometriaValida({ type, coordinates }: Geometria): Geometria | null {
  const coordenadas = sinAltura(coordinates);
  const validas = coordenadasValidas(type, coordenadas);
  return validas ? { type, coordinates: validas } : null;
}

function coordenadasValidas(tipo: string, coordenadas: unknown[]): unknown[] | null {
  switch (tipo) {
    case 'Point':
      return esPosicion(coordenadas) ? coordenadas : null;
    case 'LineString':
      return linea(coordenadas);
    case 'Polygon':
      return poligono(coordenadas);
    case 'MultiPoint':
      return lista(coordenadas, (punto) => (esPosicion(punto) ? punto : null));
    case 'MultiLineString':
      return lista(coordenadas, linea);
    case 'MultiPolygon':
      return lista(coordenadas, poligono);
    default:
      return null;
  }
}

function lista(partes: unknown[], valida: (parte: unknown[]) => unknown[] | null): unknown[] | null {
  const validas = partes.map((parte) => (Array.isArray(parte) ? valida(parte) : null));
  return validas.length > 0 && validas.every((parte) => parte !== null) ? validas : null;
}

function linea(puntos: unknown[]): unknown[] | null {
  return puntos.length >= 2 && puntos.every(esPosicion) ? puntos : null;
}

/** Hay archivos con anillos sin cerrar. */
function poligono(anillos: unknown[]): unknown[] | null {
  return lista(anillos, (anillo) => {
    if (!anillo.every(esPosicion) || anillo.length === 0) {
      return null;
    }
    const [primero] = anillo as number[][];
    const ultimo = anillo.at(-1) as number[];
    const cerrado = primero[0] === ultimo[0] && primero[1] === ultimo[1] ? anillo : [...anillo, primero];
    return cerrado.length >= 4 ? cerrado : null;
  });
}

function esPosicion(valor: unknown): boolean {
  if (!Array.isArray(valor) || valor.length !== 2) {
    return false;
  }
  const [longitud, latitud] = valor;
  return Number.isFinite(longitud) && Number.isFinite(latitud) && Math.abs(longitud) <= 180 && Math.abs(latitud) <= 90;
}
