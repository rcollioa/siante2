import { Observable, catchError, of } from 'rxjs';

/** Evita que una fuente caída tumbe el `forkJoin`: marca su área en `fallidas` y entrega `null`.
 * Crear un `fallidas` nuevo por consulta. */
export function tolerarFallos<Area extends string>(fallidas: Record<Area, boolean>) {
  return <T>(area: Area, fuente: Observable<T>): Observable<T | null> =>
    fuente.pipe(
      catchError(() => {
        fallidas[area] = true;
        return of(null);
      })
    );
}
