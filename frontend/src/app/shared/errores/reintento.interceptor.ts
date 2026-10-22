import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { retry, throwError, timer } from 'rxjs';
import { esRutaApi } from '../api/api-prefijo';

/** 502, 503 y 504 suelen ser transitorios (pod reiniciando, pool reconectando); el resto no se reintenta. */
const CODIGOS_REINTENTABLES = new Set([502, 503, 504]);
const MAX_REINTENTOS = 2;
const RETRASO_BASE_MS = 300;

/** Reintenta los GET a la API que fallan con 502/503/504, tras 300 y 600 ms.
 * No reintenta timeouts del cliente ni POST (no son idempotentes). */
export const reintentoInterceptor: HttpInterceptorFn = (req, next) => {
  if (!esRutaApi(req.url) || req.method !== 'GET') {
    return next(req);
  }
  return next(req).pipe(
    retry({
      count: MAX_REINTENTOS,
      delay: (error: unknown, intento: number) => {
        if (!(error instanceof HttpErrorResponse) || !CODIGOS_REINTENTABLES.has(error.status)) {
          return throwError(() => error);
        }
        return timer(RETRASO_BASE_MS * intento);
      }
    })
  );
};
