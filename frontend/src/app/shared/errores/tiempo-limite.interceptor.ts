import { HttpInterceptorFn } from '@angular/common/http';
import { timeout } from 'rxjs';
import { esRutaApi } from '../api/api-prefijo';

/** Con la base lenta, el backend mismo se rinde a los 30 s (`connection-timeout` de Hikari). Si una
 * ruta pesada necesita más margen, darle su propio plazo aquí según `req.url`. */
const TIEMPO_LIMITE_MS = 30_000;

/**
 * Corta las peticiones a la API que no responden a tiempo. Sin esto, un servidor colgado deja la
 * petición pendiente para siempre: la pantalla de carga global no se quita nunca y el usuario solo
 * puede recargar.
 *
 * Al vencer, la petición se aborta y el error llega a quien la pidió como cualquier otro fallo (un
 * `TimeoutError` en vez de un `HttpErrorResponse`), así que cada pantalla lo trata igual que un 503:
 * aviso de fallo y Reintentar. El plazo corre desde que se envía hasta que llega la respuesta
 * completa.
 */
export const tiempoLimiteInterceptor: HttpInterceptorFn = (req, next) => {
  if (!esRutaApi(req.url)) {
    return next(req);
  }
  return next(req).pipe(timeout({ each: TIEMPO_LIMITE_MS }));
};
