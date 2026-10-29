import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import { API_PREFIJO } from '../../../../../shared/api/api-prefijo';
import { components } from '../../../../../shared/api/contrato';
import { sinPantallaDeCarga } from '../../../../../shared/pantalla-carga/pantalla-carga.interceptor';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { Capa, CapaCatalogo, CapaNoEncontrada, ResumenCapa, SeccionCatalogo, ServidorDeMapasNoDisponible } from '../../../domain/capa';

type CapaDto = components['schemas']['Capa'];
type CapasResponse = components['schemas']['CapasResponse'];
type CatalogoResponse = components['schemas']['CatalogoResponse'];

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

  listarFavoritas(): Observable<ResumenCapa[]> {
    return this.http
      .get<CapasResponse>(`${API_PREFIJO}/capas/favoritas`, { context: sinPantallaDeCarga() })
      .pipe(map((respuesta): ResumenCapa[] => respuesta.data));
  }

  /** Sin pantalla de carga: el catálogo muestra su propio aviso mientras llega. */
  catalogo(seccion: SeccionCatalogo): Observable<CapaCatalogo[]> {
    return this.http
      .get<CatalogoResponse>(`${API_PREFIJO}/capas/catalogo`, { params: { seccion }, context: sinPantallaDeCarga() })
      .pipe(map((respuesta): CapaCatalogo[] => respuesta.data));
  }

  /** Con pantalla de carga: una capa grande puede tardar varios segundos, como en SIATE. */
  ver(id: number): Observable<Capa> {
    return this.http.get<CapaDto>(`${API_PREFIJO}/capas/${id}`).pipe(
      map((capa) => aCapa(capa)),
      catchError((error: unknown) => throwError(() => traducir(error, id)))
    );
  }
}

/** El contrato marca `servicio` y `extension` como obligatorios, pero el backend los manda en null cuando no hay. */
function aCapa(capa: CapaDto): Capa {
  const servicio = capa.servicio as CapaDto['servicio'] | null;
  return {
    ...capa,
    servicio: servicio
      ? {
          tipo: servicio.tipo,
          capas: servicio.capas.map((c) => ({ ...c, extension: c.extension ?? null })),
          urlMapa: `${API_PREFIJO}/capas/${capa.id}/wms`
        }
      : null
  };
}

function traducir(error: unknown, id: number): unknown {
  if (!(error instanceof HttpErrorResponse)) {
    return error;
  }
  if (error.status === 404) {
    return new CapaNoEncontrada(id);
  }
  return error.status === 503 && error.error?.code === 'EXTERNAL_SERVICE_UNAVAILABLE' ? new ServidorDeMapasNoDisponible(id) : error;
}
