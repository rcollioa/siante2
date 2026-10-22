import { HttpContext, HttpContextToken, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { PantallaCargaService } from './pantalla-carga.service';
import { esRutaApi } from '../api/api-prefijo';

/** Marca una petición que no debe bloquear la página con `PantallaCargaComponent`. */
export const SIN_PANTALLA_CARGA = new HttpContextToken<boolean>(() => false);

/** Contexto para que una petición no muestre la pantalla de carga global,
 * cuando la pantalla ya tiene su propio indicador. */
export function sinPantallaDeCarga(): HttpContext {
  return new HttpContext().set(SIN_PANTALLA_CARGA, true);
}

/** Muestra la pantalla de carga mientras haya peticiones a la API en curso,
 * salvo las marcadas con `sinPantallaDeCarga()`. */
export const pantallaCargaInterceptor: HttpInterceptorFn = (req, next) => {
  if (!esRutaApi(req.url) || req.context.get(SIN_PANTALLA_CARGA)) {
    return next(req);
  }
  const pantallaCarga = inject(PantallaCargaService);
  pantallaCarga.mostrar();
  return next(req).pipe(finalize(() => pantallaCarga.ocultar()));
};
