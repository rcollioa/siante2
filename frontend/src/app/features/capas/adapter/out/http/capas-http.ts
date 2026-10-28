import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import { API_PREFIJO } from '../../../../../shared/api/api-prefijo';
import { components } from '../../../../../shared/api/contrato';
import { sinPantallaDeCarga } from '../../../../../shared/pantalla-carga/pantalla-carga.interceptor';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { Capa, CapaNoEncontrada, ResumenCapa } from '../../../domain/capa';

type CapaDto = components['schemas']['Capa'];
type CapasResponse = components['schemas']['CapasResponse'];

/** Sin CacheConsultas: las capas de cada usuario cambian (también desde SIATE) y el ETag ya evita volver a
 * descargarlas si siguen iguales. Los DTO del contrato se entregan como dominio sin conversión: si el contrato
 * deja de calzar con el dominio, no compila. */
@Injectable({ providedIn: 'root' })
export class CapasHttp extends CapasRepositorio {
  private readonly http = inject(HttpClient);

  listarMias(): Observable<ResumenCapa[]> {
    return this.http
      .get<CapasResponse>(`${API_PREFIJO}/capas/mias`, { context: sinPantallaDeCarga() })
      .pipe(map((respuesta): ResumenCapa[] => respuesta.data));
  }

  /** Con pantalla de carga: una capa grande puede tardar varios segundos, como en SIATE. */
  ver(id: number): Observable<Capa> {
    return this.http.get<CapaDto>(`${API_PREFIJO}/capas/${id}`).pipe(
      map((capa): Capa => capa),
      catchError((error: unknown) =>
        throwError(() => (error instanceof HttpErrorResponse && error.status === 404 ? new CapaNoEncontrada(id) : error))
      )
    );
  }
}
