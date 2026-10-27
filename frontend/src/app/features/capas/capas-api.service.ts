import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_PREFIJO } from '../../shared/api/api-prefijo';
import { components } from '../../shared/api/contrato';
import { sinPantallaDeCarga } from '../../shared/pantalla-carga/pantalla-carga.interceptor';

export type ResumenCapa = components['schemas']['ResumenCapa'];
export type Capa = components['schemas']['Capa'];
export type ElementoCapa = components['schemas']['ElementoCapa'];
type CapasResponse = components['schemas']['CapasResponse'];

/** Sin CacheConsultas: las capas de cada usuario cambian (también desde SIATE) y el ETag ya evita volver a
 * descargarlas si siguen iguales. */
@Injectable({ providedIn: 'root' })
export class CapasApiService {
  private readonly http = inject(HttpClient);

  listarMias(): Observable<ResumenCapa[]> {
    return this.http
      .get<CapasResponse>(`${API_PREFIJO}/capas/mias`, { context: sinPantallaDeCarga() })
      .pipe(map((respuesta) => respuesta.data));
  }

  /** Con pantalla de carga: una capa grande puede tardar varios segundos, como en SIATE. */
  ver(id: number): Observable<Capa> {
    return this.http.get<Capa>(`${API_PREFIJO}/capas/${id}`);
  }
}
