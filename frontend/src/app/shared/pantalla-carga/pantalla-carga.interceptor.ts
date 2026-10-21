import { HttpContext, HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { PantallaCargaService } from './pantalla-carga.service';
import { esRutaApi } from '../api/api-prefijo';

/** Marca una petición que no debe bloquear la página con `PantallaCargaComponent`. */
export const SIN_PANTALLA_CARGA = new HttpContextToken<boolean>(() => false);

/**
 * Contexto para pasar en las opciones de `HttpClient` (`{ context: sinPantallaDeCarga() }`) cuando la
 * pantalla que hace la petición ya muestra su propio indicador de carga (p. ej. un "Consultando…" en
 * el panel). Así un cambio de filtro o abrir un detalle no tapan toda la página: quedan para la
 * pantalla de carga global solo las cargas sin las que la página no puede mostrarse.
 */
export function sinPantallaDeCarga(): HttpContext {
  return new HttpContext().set(SIN_PANTALLA_CARGA, true);
}

/**
 * Muestra `PantallaCargaComponent` mientras haya al menos una petición a la API en curso, y la
 * oculta cuando termina (éxito o error), sin que cada `*ApiService` tenga que saberlo. Solo intercepta
 * la API para no mostrar la pantalla de carga por assets u otras peticiones que no sean del backend.
 *
 * Una petición marcada con `sinPantallaDeCarga()` no la muestra; el contador de `PantallaCargaService`
 * tampoco la cuenta.
 */
export const pantallaCargaInterceptor: HttpInterceptorFn = (req, next) => {
  if (!esRutaApi(req.url) || req.context.get(SIN_PANTALLA_CARGA)) {
    return next(req);
  }
  const pantallaCarga = inject(PantallaCargaService);
  pantallaCarga.mostrar();
  return next(req).pipe(finalize(() => pantallaCarga.ocultar()));
};
