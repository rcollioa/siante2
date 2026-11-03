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

/** Lee en el navegador el archivo de una capa nueva, como SIATE: así se avisa de inmediato qué se va a importar, antes de
 * crear nada. Falla con ArchivoInvalido, con un mensaje para el usuario. */
@Injectable({ providedIn: 'root' })
export class LectorArchivos {
  private readonly mapa = inject(MapaPort);

  async leer(archivo: File): Promise<ArchivoLeido> {
    const origen = origenDe(archivo.name);
    if (!origen) {
      throw new ArchivoInvalido('El formato del archivo no está disponible: use KML, JSON o GeoJSON.');
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
