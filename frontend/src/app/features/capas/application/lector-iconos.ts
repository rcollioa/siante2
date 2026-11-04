import { Injectable } from '@angular/core';
import { excedeTamano, IconoInvalido, MAXIMO_IMAGEN_ICONO } from '../domain/icono';

export interface IconoLeido {
  archivo: string;
  imagen: string;
}

const TIPOS: Readonly<Record<string, string>> = { png: 'image/png', svg: 'image/svg+xml' };

/** Lee en el navegador el archivo de un ícono, como SIATE: solo aquí se puede saber el tamaño de un SVG. Falla con
 * IconoInvalido, con un mensaje para el usuario. */
@Injectable({ providedIn: 'root' })
export class LectorIconos {
  async leer(archivo: File): Promise<IconoLeido> {
    const extension = archivo.name.split('.').pop()?.toLowerCase() ?? '';
    const tipo = TIPOS[extension];
    if (!tipo) {
      throw new IconoInvalido('Este tipo de archivo no está soportado, los tipos de archivos permitidos son SVG y PNG.');
    }
    // El tipo sale de la extensión: el navegador no siempre lo informa, y el backend solo acepta estos dos.
    const imagen = `data:${tipo};base64,${aBase64(new Uint8Array(await archivo.arrayBuffer()))}`;
    if (imagen.length > MAXIMO_IMAGEN_ICONO) {
      throw new IconoInvalido('El archivo es demasiado grande para usarlo como ícono.');
    }
    const { ancho, alto } = await medir(imagen);
    if (excedeTamano(ancho, alto)) {
      throw new IconoInvalido('El tamaño del archivo no está soportado, los tamaños no deben superar el ancho 66 y el alto 88.');
    }
    return { archivo: archivo.name, imagen };
  }
}

function aBase64(bytes: Uint8Array): string {
  let binario = '';
  bytes.forEach((byte) => (binario += String.fromCharCode(byte)));
  return btoa(binario);
}

function medir(imagen: string): Promise<{ ancho: number; alto: number }> {
  return new Promise((resolver, rechazar) => {
    const elemento = new Image();
    elemento.onload = () => resolver({ ancho: elemento.naturalWidth, alto: elemento.naturalHeight });
    elemento.onerror = () => rechazar(new IconoInvalido('Este tipo de archivo presenta problemas de lectura.'));
    elemento.src = imagen;
  });
}
