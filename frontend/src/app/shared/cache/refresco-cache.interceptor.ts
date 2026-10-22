import { HttpInterceptorFn } from '@angular/common/http';
import { esRutaApi } from '../api/api-prefijo';

/** Parámetro de la URL que fuerza datos frescos: `/mapa?refrescar`. */
export const PARAMETRO_REFRESCAR = 'refrescar';

/** Se lee una sola vez, al cargar la página: la navegación interna quita el parámetro de la URL, pero
 * el refresco sigue valiendo para toda la carga en que se pidió. */
const refrescar = new URLSearchParams(window.location.search).has(PARAMETRO_REFRESCAR);

/** Con `?refrescar` en la URL, los GET a la API ignoran el caché del navegador (`no-cache`)
 * y la respuesta nueva reemplaza la copia guardada. */
export const refrescoCacheInterceptor: HttpInterceptorFn = (req, next) => {
  if (!refrescar || !esRutaApi(req.url) || req.method !== 'GET') {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' } }));
};
