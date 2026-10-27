import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PREFIJO } from '../../shared/api/api-prefijo';
import { components } from '../../shared/api/contrato';
import { CacheConsultas, TTL_DATOS_MS } from '../../shared/cache/cache-consultas';
import { sinPantallaDeCarga } from '../../shared/pantalla-carga/pantalla-carga.interceptor';

export type Multimedia = components['schemas']['Multimedia'];
type MultimediaResponse = components['schemas']['MultimediaResponse'];

/** Descripciones con que SIANTE guarda sus archivos de ayuda en MULTIMEDIA. */
export const ARCHIVOS_AYUDA = {
  manual: 'Manual SIATE',
  visualizarAnexos: 'Gif Visualizar',
  visualizarMetadata: 'Gif Metadata'
} as const;

@Injectable({ providedIn: 'root' })
export class AyudaApiService {
  private readonly http = inject(HttpClient);
  private readonly cache = new CacheConsultas({ ttlMs: TTL_DATOS_MS });

  listarMultimedia(): Observable<Multimedia[]> {
    return this.cache.obtener('multimedia', () =>
      this.http
        .get<MultimediaResponse>(`${API_PREFIJO}/multimedia`, { context: sinPantallaDeCarga() })
        .pipe(map((respuesta) => respuesta.data))
    );
  }

  /** URL del archivo: el navegador lo descarga (PDF) o lo muestra en un {@code <img>} (GIF) sin pasar por JS. */
  urlArchivo(id: number): string {
    return `${API_PREFIJO}/multimedia/${id}/archivo`;
  }

  /** URL del manual de usuario, o null si no está en MULTIMEDIA. */
  urlManual(): Observable<string | null> {
    return this.listarMultimedia().pipe(
      map((archivos) => {
        const manual = archivos.find((archivo) => archivo.descripcion === ARCHIVOS_AYUDA.manual);
        return manual ? this.urlArchivo(manual.id) : null;
      })
    );
  }
}
