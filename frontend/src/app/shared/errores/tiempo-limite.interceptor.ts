import { HttpInterceptorFn } from '@angular/common/http';
import { timeout } from 'rxjs';
import { esRutaApi } from '../api/api-prefijo';

/** Con la base lenta, el backend mismo se rinde a los 30 s (`connection-timeout` de Hikari). Si una
 * ruta pesada necesita más margen, darle su propio plazo aquí según `req.url`. */
const TIEMPO_LIMITE_MS = 30_000;

/** Aborta las peticiones a la API que no responden a tiempo, para no dejar fija la pantalla de carga.
 * El error llega como `TimeoutError` y cada pantalla lo trata como un 503. */
export const tiempoLimiteInterceptor: HttpInterceptorFn = (req, next) => {
  if (!esRutaApi(req.url)) {
    return next(req);
  }
  return next(req).pipe(timeout({ each: TIEMPO_LIMITE_MS }));
};
