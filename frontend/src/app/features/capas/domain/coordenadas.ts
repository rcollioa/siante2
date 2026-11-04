import { toPoint } from 'mgrs';

/** Longitud y latitud en grados (WGS84), como las guarda SIATE. */
export type Grados = [longitud: number, latitud: number];

const SEMIEJE = 6378137;
const EXCENTRICIDAD2 = 0.00669438;
const ESCALA_UTM = 0.9996;
const ESTE_FALSO = 500000;
const NORTE_FALSO_SUR = 10000000;
/** Las bandas de latitud de UTM: de la N en adelante es el hemisferio norte. */
const BANDAS = 'CDEFGHJKLMNPQRSTUVWX';
const SUR_U_OESTE = /^[SOW]$/;

/** Acepta coma decimal, común en un archivo en español. NaN si no es un número. */
export function numeroDe(texto: string): number {
  const limpio = texto.trim().replace(',', '.');
  return limpio === '' ? NaN : Number(limpio);
}

/** "19H" con su banda de latitud: sin ella no se sabe el hemisferio. Series de Snyder, como SIATE. */
export function utmAGrados(zona: string, este: number, norte: number): Grados | null {
  const partes = /^(\d{1,2})\s*([A-Za-z])$/.exec(zona.trim());
  const numero = partes ? Number(partes[1]) : 0;
  const banda = partes ? partes[2].toUpperCase() : '';
  if (numero < 1 || numero > 60 || !BANDAS.includes(banda) || !Number.isFinite(este) || !Number.isFinite(norte)) {
    return null;
  }
  const e2 = EXCENTRICIDAD2;
  const e2Prima = e2 / (1 - e2);
  const x = este - ESTE_FALSO;
  const y = banda >= 'N' ? norte : norte - NORTE_FALSO_SUR;
  const mu = y / ESCALA_UTM / (SEMIEJE * (1 - e2 / 4 - (3 * e2 ** 2) / 64 - (5 * e2 ** 3) / 256));
  const e1 = (1 - Math.sqrt(1 - e2)) / (1 + Math.sqrt(1 - e2));
  const fi =
    mu +
    ((3 * e1) / 2 - (27 * e1 ** 3) / 32) * Math.sin(2 * mu) +
    ((21 * e1 ** 2) / 16 - (55 * e1 ** 4) / 32) * Math.sin(4 * mu) +
    ((151 * e1 ** 3) / 96) * Math.sin(6 * mu) +
    ((1097 * e1 ** 4) / 512) * Math.sin(8 * mu);
  const seno = Math.sin(fi);
  const coseno = Math.cos(fi);
  const tangente = Math.tan(fi);
  const n1 = SEMIEJE / Math.sqrt(1 - e2 * seno ** 2);
  const t1 = tangente ** 2;
  const c1 = e2Prima * coseno ** 2;
  const r1 = (SEMIEJE * (1 - e2)) / (1 - e2 * seno ** 2) ** 1.5;
  const d = x / (n1 * ESCALA_UTM);
  const latitud =
    fi -
    ((n1 * tangente) / r1) *
      (d ** 2 / 2 -
        ((5 + 3 * t1 + 10 * c1 - 4 * c1 ** 2 - 9 * e2Prima) * d ** 4) / 24 +
        ((61 + 90 * t1 + 298 * c1 + 45 * t1 ** 2 - 252 * e2Prima - 3 * c1 ** 2) * d ** 6) / 720);
  const longitud =
    (d - ((1 + 2 * t1 + c1) * d ** 3) / 6 + ((5 - 2 * c1 + 28 * t1 - 3 * c1 ** 2 + 8 * e2Prima + 24 * t1 ** 2) * d ** 5) / 120) / coseno;
  return [(numero - 1) * 6 - 180 + 3 + (longitud * 180) / Math.PI, (latitud * 180) / Math.PI];
}

/** Grados, minutos y, si los hay, segundos: "33° 26' 15'' S" o "33 26 15 S". Como SIATE, S y O (también W) son negativos. */
function sexagesimalAGrados(texto: string, conSegundos: boolean): number {
  const partes = texto.trim().replace(',', '.').split(/[^\d\w.-]+/).filter((parte) => parte !== '');
  const direccion = /[A-Za-z]$/.test(partes.at(-1) ?? '') ? partes.pop()!.toUpperCase() : '';
  if (partes.length !== (conSegundos ? 3 : 2)) {
    return NaN;
  }
  const [grados, minutos, segundos] = partes.map(Number);
  const valor = Math.abs(grados) + minutos / 60 + (conSegundos ? segundos / 3600 : 0);
  return SUR_U_OESTE.test(direccion) || grados < 0 ? -valor : valor;
}

export function gmsAGrados(latitud: string, longitud: string): Grados {
  return [sexagesimalAGrados(longitud, true), sexagesimalAGrados(latitud, true)];
}

export function gmdAGrados(latitud: string, longitud: string): Grados {
  return [sexagesimalAGrados(longitud, false), sexagesimalAGrados(latitud, false)];
}

/** La cuadrícula militar; null si el texto no es una coordenada. */
export function mgrsAGrados(texto: string): Grados | null {
  try {
    const [longitud, latitud] = toPoint(texto.replace(/\s+/g, '').toUpperCase());
    return [longitud, latitud];
  } catch {
    return null;
  }
}
