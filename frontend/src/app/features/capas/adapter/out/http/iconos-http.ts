import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import { API_PREFIJO } from '../../../../../shared/api/api-prefijo';
import { components } from '../../../../../shared/api/contrato';
import { sinPantallaDeCarga } from '../../../../../shared/pantalla-carga/pantalla-carga.interceptor';
import { IconosRepositorio } from '../../../application/port/out/iconos.repositorio';
import { CambiosRechazados } from '../../../domain/capa';
import { DatosIcono, Icono } from '../../../domain/icono';

type IconoDto = components['schemas']['Icono'];
type IconosResponse = components['schemas']['IconosResponse'];
type DatosIconoDto = components['schemas']['DatosIcono'];

@Injectable({ providedIn: 'root' })
export class IconosHttp extends IconosRepositorio {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_PREFIJO}/iconos`;

  listar(): Observable<Icono[]> {
    return this.http
      .get<IconosResponse>(this.url, { context: sinPantallaDeCarga() })
      .pipe(map((respuesta): Icono[] => respuesta.data));
  }

  crear(datos: DatosIcono): Observable<Icono> {
    const cuerpo: DatosIconoDto = datos;
    return this.http.post<IconoDto>(this.url, cuerpo, { context: sinPantallaDeCarga() }).pipe(
      map((icono): Icono => icono),
      catchError(traducir)
    );
  }

  editar(id: number, datos: DatosIcono): Observable<void> {
    const cuerpo: DatosIconoDto = datos;
    return this.http.patch<void>(`${this.url}/${id}`, cuerpo, { context: sinPantallaDeCarga() }).pipe(catchError(traducir));
  }

  eliminar(id: number): Observable<void> {
    return this.http.post<void>(`${this.url}/${id}/eliminar`, null, { context: sinPantallaDeCarga() });
  }
}

function traducir(error: unknown): Observable<never> {
  const rechazo = error instanceof HttpErrorResponse && error.status === 400 && typeof error.error?.message === 'string';
  return throwError(() => (rechazo ? new CambiosRechazados(error.error.message) : error));
}
