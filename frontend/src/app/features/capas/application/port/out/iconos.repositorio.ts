import { Observable } from 'rxjs';
import { DatosIcono, Icono } from '../../../domain/icono';

/** Los íconos de los marcadores guardados en SIATE. Las escrituras fallan con CambiosRechazados si los datos no son
 * válidos. */
export abstract class IconosRepositorio {
  abstract listar(): Observable<Icono[]>;
  abstract crear(datos: DatosIcono): Observable<Icono>;
  abstract editar(id: number, datos: DatosIcono): Observable<void>;
  abstract eliminar(id: number): Observable<void>;
}
