import { inject, Injectable } from '@angular/core';
import { MapaPort } from '../../mapa/application/port/out/mapa.port';
import {
  ArchivoInvalido,
  ArchivoLeido,
  decodificar,
  leerGeoJson,
  MAXIMO_BYTES_ARCHIVO,
  origenDe,
  revisar,
  tieneCarpetas
} from '../domain/archivo-geografico';
import { armarTabla, leerCsv, TablaLeida } from '../domain/tabla-geografica';
import { LectorExcel } from './port/out/lector-excel.port';

/** Lee en el navegador el archivo de una capa nueva, como SIATE: así se avisa de inmediato qué se va a importar, antes de
 * crear nada. Falla con ArchivoInvalido, con un mensaje para el usuario. */
@Injectable({ providedIn: 'root' })
export class LectorArchivos {
  private readonly mapa = inject(MapaPort);
  private readonly excel = inject(LectorExcel);

  /** Un Excel o un CSV, para elegir después en qué columnas está la ubicación. */
  async leerTabla(archivo: File): Promise<TablaLeida> {
    if (archivo.size > MAXIMO_BYTES_ARCHIVO) {
      throw new ArchivoInvalido('El archivo excede el tamaño máximo de 30 MB.');
    }
    const contenido = await archivo.arrayBuffer();
    if (archivo.name.toLowerCase().endsWith('.csv')) {
      return armarTabla('CSV', leerCsv(decodificar(contenido)));
    }
    try {
      return armarTabla('XLSX', await this.excel.leer(contenido));
    } catch (error) {
      throw error instanceof ArchivoInvalido ? error : new ArchivoInvalido('El archivo Excel no se pudo leer.');
    }
  }

  async leer(archivo: File): Promise<ArchivoLeido> {
    const origen = origenDe(archivo.name);
    if (!origen) {
      throw new ArchivoInvalido(formatoNoDisponible(archivo.name));
    }
    if (archivo.size > MAXIMO_BYTES_ARCHIVO) {
      throw new ArchivoInvalido('El archivo excede el tamaño máximo de 30 MB.');
    }
    const texto = decodificar(await archivo.arrayBuffer());
    return origen === 'KML'
      ? revisar(await this.mapa.leerKml(texto), origen, tieneCarpetas(texto))
      : revisar(leerGeoJson(texto), origen, false);
  }
}

/** El formato binario antiguo de Excel requeriría otra librería, y Excel lo guarda como .xlsx. */
function formatoNoDisponible(nombre: string): string {
  return nombre.toLowerCase().endsWith('.xls')
    ? 'El formato .xls antiguo no está disponible: guárdelo como .xlsx desde Excel.'
    : 'El formato del archivo no está disponible: use KML, JSON, GeoJSON, CSV o Excel (.xlsx).';
}
