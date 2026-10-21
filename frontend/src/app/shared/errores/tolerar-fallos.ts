import { Observable, catchError, of } from 'rxjs';

/**
 * Envuelve una fuente de datos para que un error no tumbe al `forkJoin` que la agrupa (un área caída
 * solo deja sin datos SU parte, aunque las demás respondan bien), pero SIN esconder que falló: marca
 * el área en `fallidas` y entrega `null`. Así la pantalla puede distinguir "no hay datos" de "no se
 * pudieron cargar" y ofrecer Reintentar, en vez de decir "Sin datos" en ambos casos.
 *
 * Uso: crear un `fallidas` nuevo por consulta, pasarlo acá, y al terminar publicarlo en una señal.
 *
 * @example
 * const fallidas = { resumen: false, detalle: false };
 * const tolerar = tolerarFallos(fallidas);
 * forkJoin({ resumen: tolerar('resumen', api.resumen()), ... }).subscribe(() => this.fallos.set(fallidas));
 */
export function tolerarFallos<Area extends string>(fallidas: Record<Area, boolean>) {
  return <T>(area: Area, fuente: Observable<T>): Observable<T | null> =>
    fuente.pipe(
      catchError(() => {
        fallidas[area] = true;
        return of(null);
      })
    );
}
