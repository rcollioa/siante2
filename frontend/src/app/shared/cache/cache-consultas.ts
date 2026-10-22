import { Observable, shareReplay } from 'rxjs';

/** Vigencia sugerida para datos de negocio en memoria: suficiente para que una pestaña abierta mucho
 * tiempo no quede con datos viejos. Ajustar según cada cuánto se actualicen los datos del proyecto. */
export const TTL_DATOS_MS = 30 * 60 * 1000;

interface Entrada {
  consulta: Observable<unknown>;
  creada: number;
}

interface OpcionesCache {
  /** Vigencia de cada entrada. Sin valor, dura toda la sesión (hasta recargar la página). */
  ttlMs?: number;
  /** Máximo de entradas; al pasarse, se descarta la menos usada. */
  maxEntradas?: number;
}

/** Caché en memoria por clave para los `*ApiService`: comparte la petición en curso y no guarda errores.
 * Los objetos se comparten entre pantallas: no mutarlos, usar una copia. */
export class CacheConsultas {
  private readonly entradas = new Map<string, Entrada>();

  constructor(private readonly opciones: OpcionesCache = {}) {}

  obtener<T>(clave: string, crear: () => Observable<T>): Observable<T> {
    const { ttlMs, maxEntradas = 200 } = this.opciones;
    const ahora = Date.now();

    const existente = this.entradas.get(clave);
    if (existente && (ttlMs === undefined || ahora - existente.creada < ttlMs)) {
      // Reinsertarla la deja como la más reciente: el orden del Map es el orden de uso.
      this.entradas.delete(clave);
      this.entradas.set(clave, existente);
      return existente.consulta as Observable<T>;
    }

    const consulta = crear().pipe(shareReplay({ bufferSize: 1, refCount: true }));
    this.entradas.delete(clave);
    this.entradas.set(clave, { consulta, creada: ahora });
    if (this.entradas.size > maxEntradas) {
      const masAntigua = this.entradas.keys().next().value;
      if (masAntigua !== undefined) {
        this.entradas.delete(masAntigua);
      }
    }
    return consulta;
  }
}
