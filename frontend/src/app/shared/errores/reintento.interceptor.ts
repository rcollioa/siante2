import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { retry, throwError, timer } from 'rxjs';
import { esRutaApi } from '../api/api-prefijo';

/** 502, 503 y 504 suelen ser transitorios (pod reiniciando, pool reconectando); el resto no se reintenta. */
const CODIGOS_REINTENTABLES = new Set([502, 503, 504]);
const MAX_REINTENTOS = 2;
const RETRASO_BASE_MS = 300;
/** El backend ya reintentó al servicio externo: repetir aquí multiplicaría las llamadas a terceros
 * (Nominatim admite una por segundo) y la espera del usuario. */
const SERVICIO_EXTERNO_CAIDO = 'EXTERNAL_SERVICE_UNAVAILABLE';

/** Reintenta los GET a la API que fallan con 502/503/504, tras 300 y 600 ms.
 * No reintenta timeouts del cliente, POST (no son idempotentes) ni un servicio externo caído. */
export const reintentoInterceptor: HttpInterceptorFn = (req, next) => {
  if (!esRutaApi(req.url) || req.method !== 'GET') {
    return next(req);
  }
  return next(req).pipe(
    retry({
      count: MAX_REINTENTOS,
      delay: (error: unknown, intento: number) => {
        if (!esReintentable(error)) {
          return throwError(() => error);
        }
        return timer(RETRASO_BASE_MS * intento);
      }
    })
  );
};

function esReintentable(error: unknown): boolean {
  return (
    error instanceof HttpErrorResponse &&
    CODIGOS_REINTENTABLES.has(error.status) &&
    error.error?.code !== SERVICIO_EXTERNO_CAIDO
  );
}
