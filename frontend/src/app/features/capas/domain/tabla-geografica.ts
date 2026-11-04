import { Geometria, ElementoLeido } from '../../mapa/domain/mapa';
import { ArchivoInvalido, ArchivoLeido, revisar } from './archivo-geografico';
import { Grados, gmdAGrados, gmsAGrados, mgrsAGrados, numeroDe, utmAGrados } from './coordenadas';

export type OrigenTabla = 'CSV' | 'XLSX';

/** Un Excel o un CSV leído: la primera fila son los títulos. Falta decir en qué columnas está la ubicación. */
export interface TablaLeida {
  origen: OrigenTabla;
  titulos: string[];
  filas: string[][];
}

export type TipoUbicacion = 'GEODESICAS' | 'UTM' | 'GMS' | 'GMD' | 'MGRS';

/** Las opciones de SIATE, con lo que cada una necesita y cómo reconocerlo en los títulos. */
export const TIPOS_UBICACION: readonly { id: TipoUbicacion; nombre: string; columnas: readonly { nombre: string; titulo: RegExp }[] }[] = [
  {
    id: 'GEODESICAS',
    nombre: 'Geodésicas',
    columnas: [
      { nombre: 'Latitud', titulo: /^lat/i },
      { nombre: 'Longitud', titulo: /^(lon|lng)/i }
    ]
  },
  {
    id: 'UTM',
    nombre: 'UTM',
    columnas: [
      { nombre: 'Zona, con su letra (19H)', titulo: /^(zona|zone|huso)/i },
      { nombre: 'Este', titulo: /^(este|east|x$)/i },
      { nombre: 'Norte', titulo: /^(norte|north|y$)/i }
    ]
  },
  {
    id: 'GMS',
    nombre: 'Grados, minutos, segundos',
    columnas: [
      { nombre: 'Latitud', titulo: /^lat/i },
      { nombre: 'Longitud', titulo: /^(lon|lng)/i }
    ]
  },
  {
    id: 'MGRS',
    nombre: 'Sistema de referencia de cuadrícula militar',
    columnas: [{ nombre: 'Coordenada', titulo: /(mgrs|cuadr[ií]cula|coordenada)/i }]
  },
  {
    id: 'GMD',
    nombre: 'Grados, minutos decimales',
    columnas: [
      { nombre: 'Latitud', titulo: /^lat/i },
      { nombre: 'Longitud', titulo: /^(lon|lng)/i }
    ]
  }
];

const SIN_GEOMETRIA: Geometria = { type: '', coordinates: [] };
const NOMBRE = /^(nombre|name)$/i;
const SEPARADORES = [';', ',', '\t'];

function definicion(tipo: TipoUbicacion) {
  return TIPOS_UBICACION.find(({ id }) => id === tipo)!;
}

/** La columna que parece corresponder a cada dato, o -1 para que la elija el usuario. */
export function columnasSugeridas({ titulos }: TablaLeida, tipo: TipoUbicacion): number[] {
  return definicion(tipo).columnas.map(({ titulo }) => titulos.findIndex((texto) => titulo.test(texto.trim())));
}

/** El primero con coordenadas en sus títulos; si no reconoce ninguno, el primero de la lista. */
export function tipoSugerido(tabla: TablaLeida): TipoUbicacion {
  return TIPOS_UBICACION.find(({ id }) => !columnasSugeridas(tabla, id).includes(-1))?.id ?? 'GEODESICAS';
}

/** Los títulos y las filas de un archivo, sin filas vacías. Falla con ArchivoInvalido si no hay datos. */
export function armarTabla(origen: OrigenTabla, celdas: readonly (readonly string[])[]): TablaLeida {
  const conDatos = celdas.filter((fila) => fila.some((celda) => celda.trim() !== ''));
  if (conDatos.length < 2) {
    throw new ArchivoInvalido('El archivo debe tener una fila de títulos y al menos una fila de datos.');
  }
  const ancho = Math.max(...conDatos.map((fila) => fila.length));
  const [titulos, ...filas] = conDatos.map((fila) => Array.from({ length: ancho }, (_, i) => fila[i]?.trim() ?? ''));
  return { origen, titulos, filas };
}

/** Comillas como en RFC 4180, con el separador que Excel usa en español (;) o los otros comunes. */
export function leerCsv(texto: string): string[][] {
  const limpio = texto.replace(/^\uFEFF/, '');
  const separador = separadorDe(limpio);
  const filas: string[][] = [];
  let fila: string[] = [];
  let celda = '';
  let enComillas = false;
  for (let i = 0; i < limpio.length; i++) {
    const caracter = limpio[i];
    if (enComillas) {
      if (caracter === '"' && limpio[i + 1] === '"') {
        celda += '"';
        i++;
      } else if (caracter === '"') {
        enComillas = false;
      } else {
        celda += caracter;
      }
    } else if (caracter === '"') {
      enComillas = true;
    } else if (caracter === separador) {
      fila.push(celda);
      celda = '';
    } else if (caracter === '\n') {
      filas.push([...fila, celda.replace(/\r$/, '')]);
      fila = [];
      celda = '';
    } else {
      celda += caracter;
    }
  }
  return [...filas, [...fila, celda.replace(/\r$/, '')]];
}

/** El que más aparece en la primera línea, fuera de comillas. */
function separadorDe(texto: string): string {
  const linea = (texto.split('\n', 1)[0] ?? '').replace(/"[^"]*"/g, '');
  return SEPARADORES.map((separador) => ({ separador, veces: linea.split(separador).length })).sort((a, b) => b.veces - a.veces)[0].separador;
}

/** Cada fila es un marcador. Como en SIATE, las demás columnas quedan como metadatos y las filas sin una ubicación válida se
 * omiten; a diferencia de SIATE, se avisa cuántas son. El nombre es la columna con ese título o la primera sin ubicación. */
export function convertirTabla(tabla: TablaLeida, tipo: TipoUbicacion, columnas: readonly number[]): ArchivoLeido {
  const nombre = columnaDeNombre(tabla, columnas);
  const leidos = tabla.filas.map((fila): ElementoLeido => {
    const grados = ubicacion(tipo, columnas.map((columna) => fila[columna] ?? ''));
    return {
      geometria: grados ? { type: 'Point', coordinates: grados } : SIN_GEOMETRIA,
      radio: 0,
      nombre: nombre < 0 ? '' : fila[nombre],
      descripcion: '',
      estilo: { color: '', grosor: 0, relleno: '', icono: '' },
      metadatos: tabla.titulos.flatMap((titulo, i) => (i === nombre || !titulo ? [] : [{ nombre: titulo, valor: fila[i] }]))
    };
  });
  return revisar(leidos, tabla.origen, false);
}

function columnaDeNombre({ titulos }: TablaLeida, columnas: readonly number[]): number {
  const conTitulo = titulos.findIndex((titulo) => NOMBRE.test(titulo.trim()));
  return conTitulo >= 0 ? conTitulo : titulos.findIndex((_, i) => !columnas.includes(i));
}

function ubicacion(tipo: TipoUbicacion, valores: readonly string[]): Grados | null {
  const [primero, segundo, tercero] = valores;
  switch (tipo) {
    case 'GEODESICAS':
      return [numeroDe(segundo), numeroDe(primero)];
    case 'UTM':
      return utmAGrados(primero, numeroDe(segundo), numeroDe(tercero));
    case 'GMS':
      return gmsAGrados(primero, segundo);
    case 'GMD':
      return gmdAGrados(primero, segundo);
    case 'MGRS':
      return mgrsAGrados(primero);
  }
}
