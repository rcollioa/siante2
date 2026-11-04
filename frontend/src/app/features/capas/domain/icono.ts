/** Un ícono para los marcadores; `imagen` es una URL data:, que el elemento copia al usarla. Solo los propios se cambian. */
export interface Icono {
  id: number;
  nombre: string;
  archivo: string;
  imagen: string;
  propio: boolean;
}

export type DatosIcono = Pick<Icono, 'nombre' | 'archivo' | 'imagen'>;

/** Lo que guarda SIATE para el marcador normal. */
export const ICONO_POR_DEFECTO = './assets/img/marker.svg';

/** Los de SIATE, y el largo de una imagen que acepta el backend. */
export const MAXIMO_NOMBRE_ICONO = 200;
export const MAXIMO_ANCHO_ICONO = 66;
export const MAXIMO_ALTO_ICONO = 88;
export const MAXIMO_IMAGEN_ICONO = 32000;

/** El archivo no sirve como ícono; el mensaje es para el usuario. */
export class IconoInvalido extends Error {}

export function esImagenIncrustada(icono: string): boolean {
  return icono.startsWith('data:image/');
}

/** Sin contar los espacios a los lados, que no se guardan. */
export function errorDeNombreIcono(nombre: string): string {
  const largo = nombre.trim().length;
  if (largo === 0) {
    return 'Nombre requerido.';
  }
  return largo > MAXIMO_NOMBRE_ICONO ? `El nombre no puede tener más de ${MAXIMO_NOMBRE_ICONO} caracteres.` : '';
}

/** A diferencia de SIATE, que solo rechazaba si excedía las dos medidas a la vez, basta con exceder una. */
export function excedeTamano(ancho: number, alto: number): boolean {
  return ancho > MAXIMO_ANCHO_ICONO || alto > MAXIMO_ALTO_ICONO;
}
