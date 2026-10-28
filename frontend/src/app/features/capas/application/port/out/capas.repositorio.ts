import { Observable } from 'rxjs';
import { Capa, ResumenCapa } from '../../../domain/capa';

/** Las capas guardadas en SIATE. */
export abstract class CapasRepositorio {
  abstract listarMias(): Observable<ResumenCapa[]>;

  /** Falla con CapaNoEncontrada si no existe o el usuario no puede verla. */
  abstract ver(id: number): Observable<Capa>;
}
