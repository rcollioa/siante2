import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { retry, throwError, timer } from 'rxjs';
import { esRutaApi } from '../api/api-prefijo';

/** 502 (Bad Gateway), 503 (Service Unavailable) y 504 (Gateway Timeout): típicamente transitorios —
 * el pod del backend recién reiniciado, el pool de conexiones a la base recuperándose, el ingress sin
 * upstream un instante. Cualquier otro código (400, 404, 500 genérico) es una respuesta real del
 * servidor y no se reintenta: reintentar un 500 de una excepción de negocio no lo arregla, solo
 * retrasa el aviso al usuario. */
const CODIGOS_REINTENTABLES = new Set([502, 503, 504]);
const MAX_REINTENTOS = 2;
const RETRASO_BASE_MS = 300;

/**
 * Reintenta automáticamente los GET a la API que fallan con 502/503/504, con una espera corta y
 * creciente entre intentos (300 ms, 600 ms) antes de rendirse. Con esto, un corte de un par de
 * segundos (p. ej. el pool de Hikari recién reconectando) se resuelve solo, sin que el usuario vea el
 * aviso de fallo ni tenga que apretar Reintentar — que sigue disponible igual si los reintentos
 * también fallan.
 *
 * A propósito NO reintenta un timeout del cliente (`TimeoutError` de `tiempoLimiteInterceptor`, que no
 * es un `HttpErrorResponse`): si el servidor no respondió en 30-60 s, reintentar de inmediato solo
 * suma otra espera igual de larga antes del mismo resultado. Tampoco reintenta POST: no son
 * idempotentes en general, y acá no vale la pena distinguir cuáles sí lo son.
 */
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
