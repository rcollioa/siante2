import { HttpInterceptorFn } from '@angular/common/http';
import { esRutaApi } from '../api/api-prefijo';

/** Parámetro de la URL que fuerza datos frescos: `/inicio?refrescar`. */
export const PARAMETRO_REFRESCAR = 'refrescar';

/** Se lee una sola vez, al cargar la página: la navegación interna quita el parámetro de la URL, pero
 * el refresco sigue valiendo para toda la carga en que se pidió. */
const refrescar = new URLSearchParams(window.location.search).has(PARAMETRO_REFRESCAR);

/**
 * Con `?refrescar` en la URL, los GET a la API de esa carga de página van directo al backend sin
 * pasar por el caché del navegador (el `Cache-Control` de `CacheControlAdvice`, `app.cache.max-age`).
 * Sirve para ver de inmediato una carga nueva en la base sin esperar a que venza ese caché ni pedir
 * Ctrl+F5.
 *
 * - El caché en memoria de los `*ApiService` (`CacheConsultas`) nace vacío en cada carga de página,
 *   así que tampoco hay que limpiarlo: dentro de la misma carga se sigue usando, y una misma consulta
 *   no se pide dos veces.
 * - Con `Cache-Control: no-cache` el navegador no usa la copia guardada, y la respuesta nueva la
 *   reemplaza: las cargas siguientes, ya sin `?refrescar`, también ven los datos nuevos.
 */
export const refrescoCacheInterceptor: HttpInterceptorFn = (req, next) => {
  if (!refrescar || !esRutaApi(req.url) || req.method !== 'GET') {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' } }));
};
