import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, throwError } from 'rxjs';
import { API_PREFIJO } from '../../../../../shared/api/api-prefijo';
import { components } from '../../../../../shared/api/contrato';
import { sinPantallaDeCarga } from '../../../../../shared/pantalla-carga/pantalla-carga.interceptor';
import { CapasRepositorio } from '../../../application/port/out/capas.repositorio';
import { PuntoWms } from '../../../../mapa/domain/mapa';
import {
  CambiosRechazados,
  Capa,
  CapaCatalogo,
  CapaNoEncontrada,
  CapaPropia,
  EdicionCapa,
  ElementoDeServicio,
  NuevaCapa,
  ResumenCapa,
  SeccionCatalogo,
  ServidorDeMapasNoDisponible,
  VisibilidadEditable
} from '../../../domain/capa';
import { CambiosElementos } from '../../../domain/cambios-elementos';

type CapaDto = components['schemas']['Capa'];
type CapasResponse = components['schemas']['CapasResponse'];
type CambioVisibilidad = components['schemas']['CambioVisibilidad'];
type EdicionCapaDto = components['schemas']['EdicionCapa'];
type MisCapasResponse = components['schemas']['MisCapasResponse'];
type CapaPropiaDto = components['schemas']['CapaPropia'];
type NuevaCapaDto = components['schemas']['NuevaCapa'];
type CatalogoResponse = components['schemas']['CatalogoResponse'];
type ElementosDeServicioResponse = components['schemas']['ElementosDeServicioResponse'];
type CambiosElementosDto = components['schemas']['CambiosElementos'];
type ElementosGuardados = components['schemas']['ElementosGuardados'];

/** Sin CacheConsultas: las capas de cada usuario cambian (también desde SIATE) y el ETag ya evita volver a
 * descargarlas si siguen iguales. Los DTO del contrato se entregan como dominio sin conversión: si el contrato
 * deja de calzar con el dominio, no compila. */
@Injectable({ providedIn: 'root' })
export class CapasHttp extends CapasRepositorio {
  private readonly http = inject(HttpClient);

  listarMias(): Observable<CapaPropia[]> {
    return this.http
      .get<MisCapasResponse>(`${API_PREFIJO}/capas/mias`, { context: sinPantallaDeCarga() })
      .pipe(map((respuesta): CapaPropia[] => respuesta.data));
  }

  /** Las escrituras van sin pantalla de carga: su diálogo muestra que se está guardando. */
  crear(nueva: NuevaCapa): Observable<CapaPropia> {
    const cuerpo: NuevaCapaDto = nueva;
    return this.http
      .post<CapaPropiaDto>(`${API_PREFIJO}/capas`, cuerpo, { context: sinPantallaDeCarga() })
      .pipe(map((capa): CapaPropia => capa));
  }

  editar(id: number, edicion: EdicionCapa): Observable<void> {
    const cuerpo: EdicionCapaDto = edicion;
    return this.escribir(id, this.http.patch<void>(`${API_PREFIJO}/capas/${id}`, cuerpo, { context: sinPantallaDeCarga() }));
  }

  cambiarVisibilidad(id: number, visibilidad: VisibilidadEditable): Observable<void> {
    const cuerpo: CambioVisibilidad = { visibilidad };
    return this.escribir(
      id,
      this.http.patch<void>(`${API_PREFIJO}/capas/${id}/visibilidad`, cuerpo, { context: sinPantallaDeCarga() })
    );
  }

  eliminar(id: number): Observable<void> {
    return this.escribir(
      id,
      this.http.post<void>(`${API_PREFIJO}/capas/${id}/eliminar`, null, { context: sinPantallaDeCarga() })
    );
  }

  /** Con pantalla de carga, como en SIATE: mientras se guarda no se puede seguir dibujando. El contrato marca `geometries`
   * como obligatorio, pero una geometría dibujada no lo lleva. */
  guardarElementos(id: number, cambios: CambiosElementos): Observable<number[]> {
    return this.http
      .post<ElementosGuardados>(`${API_PREFIJO}/capas/${id}/elementos`, cambios as unknown as CambiosElementosDto)
      .pipe(
        map((respuesta) => respuesta.nuevos),
        catchError((error: unknown) => throwError(() => traducirRechazo(error, id)))
      );
  }

  private escribir(id: number, pedido: Observable<void>): Observable<void> {
    return pedido.pipe(catchError((error: unknown) => throwError(() => traducir(error, id))));
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

  /** Sin pantalla de carga: el panel de la capa avisa que está consultando, y el mapa sigue usable. */
  consultarPunto(punto: PuntoWms): Observable<ElementoDeServicio[]> {
    const params = {
      LAYERS: punto.capas.join(','),
      BBOX: punto.bbox,
      WIDTH: punto.ancho,
      HEIGHT: punto.alto,
      CRS: punto.crs,
      VERSION: punto.version,
      I: punto.i,
      J: punto.j
    };
    return this.http
      .get<ElementosDeServicioResponse>(`${API_PREFIJO}/capas/${punto.capa}/wms/info`, { params, context: sinPantallaDeCarga() })
      .pipe(
        map((respuesta): ElementoDeServicio[] => respuesta.data),
        catchError((error: unknown) => throwError(() => traducir(error, punto.capa)))
      );
  }
}

/** El contrato marca `servicio`, `extension` y `grilla` como obligatorios, pero el backend los manda en null cuando
 * no hay. */
function aCapa(capa: CapaDto): Capa {
  const servicio = capa.servicio as CapaDto['servicio'] | null;
  return {
    ...capa,
    servicio: servicio
      ? {
          tipo: servicio.tipo,
          capas: servicio.capas.map((c) => ({ ...c, extension: c.extension ?? null })),
          urlMapa: `${API_PREFIJO}/capas/${capa.id}/${servicio.tipo === 'WMTS' ? 'wmts' : 'wms'}`,
          disponible: servicio.disponible,
          grilla: servicio.grilla ?? null
        }
      : null
  };
}

/** Un 400 trae el mensaje que explica qué dato no sirvió. */
function traducirRechazo(error: unknown, id: number): unknown {
  return error instanceof HttpErrorResponse && error.status === 400 && typeof error.error?.message === 'string'
    ? new CambiosRechazados(error.error.message)
    : traducir(error, id);
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
