import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PREFIJO } from '../../shared/api/api-prefijo';
import { components } from '../../shared/api/contrato';
import { CacheConsultas, TTL_DATOS_MS } from '../../shared/cache/cache-consultas';
import { sinPantallaDeCarga } from '../../shared/pantalla-carga/pantalla-carga.interceptor';
import { ZonaVisible } from '../mapa/mapa.service';

export type Direccion = components['schemas']['Direccion'];
type DireccionesResponse = components['schemas']['DireccionesResponse'];

@Injectable({ providedIn: 'root' })
export class DireccionesApiService {
  private readonly http = inject(HttpClient);
  private readonly cache = new CacheConsultas({ ttlMs: TTL_DATOS_MS });

  /** Búsqueda completa al confirmar (Nominatim). */
  buscar(texto: string): Observable<Direccion[]> {
    return this.consultar('direcciones', texto);
  }

  /** Sugerencias mientras se escribe (Photon, en el backend); las cercanas a la zona del mapa primero. */
  sugerir(texto: string, zona: ZonaVisible): Observable<Direccion[]> {
    // Como el backend, a unos 1 km: mover un poco el mapa no cambia la consulta y la caché la reconoce.
    return this.consultar('direcciones/sugerencias', texto, {
      latitud: redondear(zona.latitud),
      longitud: redondear(zona.longitud),
      zoom: Math.round(zona.zoom)
    });
  }

  /** Como el backend, espacios y mayúsculas no cambian la búsqueda: así la caché la reconoce. */
  private consultar(ruta: string, texto: string, zona: Record<string, number> = {}): Observable<Direccion[]> {
    const normalizado = texto.trim().replace(/\s+/g, ' ').toLowerCase();
    const params = new HttpParams({ fromObject: { texto: normalizado, ...zona } });
    return this.cache.obtener(`${ruta}?${params}`, () =>
      this.http
        .get<DireccionesResponse>(`${API_PREFIJO}/${ruta}`, { params, context: sinPantallaDeCarga() })
        .pipe(map((respuesta) => respuesta.data))
    );
  }
}

function redondear(grados: number): number {
  return Math.round(grados * 100) / 100;
}
