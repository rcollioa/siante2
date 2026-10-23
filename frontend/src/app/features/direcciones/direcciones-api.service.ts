import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PREFIJO } from '../../shared/api/api-prefijo';
import { components } from '../../shared/api/contrato';
import { CacheConsultas, TTL_DATOS_MS } from '../../shared/cache/cache-consultas';
import { sinPantallaDeCarga } from '../../shared/pantalla-carga/pantalla-carga.interceptor';

export type Direccion = components['schemas']['Direccion'];
type DireccionesResponse = components['schemas']['DireccionesResponse'];

@Injectable({ providedIn: 'root' })
export class DireccionesApiService {
  private readonly http = inject(HttpClient);
  private readonly cache = new CacheConsultas({ ttlMs: TTL_DATOS_MS });

  buscar(texto: string): Observable<Direccion[]> {
    return this.cache.obtener(`direcciones:${texto}`, () =>
      this.http
        .get<DireccionesResponse>(`${API_PREFIJO}/direcciones`, { params: { texto }, context: sinPantallaDeCarga() })
        .pipe(map((respuesta) => respuesta.data))
    );
  }
}
