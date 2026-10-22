import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PREFIJO } from '../../shared/api/api-prefijo';
import { components } from '../../shared/api/contrato';
import { CacheConsultas, TTL_DATOS_MS } from '../../shared/cache/cache-consultas';
import { sinPantallaDeCarga } from '../../shared/pantalla-carga/pantalla-carga.interceptor';

export type Categoria = components['schemas']['Categoria'];
type CategoriasResponse = components['schemas']['CategoriasResponse'];

@Injectable({ providedIn: 'root' })
export class CategoriasApiService {
  private readonly http = inject(HttpClient);
  private readonly cache = new CacheConsultas({ ttlMs: TTL_DATOS_MS });

  listar(): Observable<Categoria[]> {
    return this.cache.obtener('categorias', () =>
      this.http
        .get<CategoriasResponse>(`${API_PREFIJO}/categorias`, { context: sinPantallaDeCarga() })
        .pipe(map((respuesta) => respuesta.data))
    );
  }
}
