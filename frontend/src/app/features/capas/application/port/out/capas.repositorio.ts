import { Observable } from 'rxjs';
import { Capa, CapaCatalogo, ResumenCapa, SeccionCatalogo } from '../../../domain/capa';

/** Las capas guardadas en SIATE. */
export abstract class CapasRepositorio {
  abstract listarMias(): Observable<ResumenCapa[]>;

  /** Favoritas y formales elegidas por el usuario, que todavía puede ver. */
  abstract listarFavoritas(): Observable<ResumenCapa[]>;

  abstract catalogo(seccion: SeccionCatalogo): Observable<CapaCatalogo[]>;

  /** Falla con CapaNoEncontrada si no existe o el usuario no puede verla. */
  abstract ver(id: number): Observable<Capa>;
}
