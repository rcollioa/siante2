import { Observable } from 'rxjs';
import { PuntoWms } from '../../../../mapa/domain/mapa';
import { Capa, CapaCatalogo, ElementoDeServicio, ResumenCapa, SeccionCatalogo } from '../../../domain/capa';

/** Las capas guardadas en SIATE. */
export abstract class CapasRepositorio {
  abstract listarMias(): Observable<ResumenCapa[]>;

  /** Favoritas y formales elegidas por el usuario, que todavía puede ver. */
  abstract listarFavoritas(): Observable<ResumenCapa[]>;

  abstract catalogo(seccion: SeccionCatalogo): Observable<CapaCatalogo[]>;

  /** Falla con CapaNoEncontrada si no existe o el usuario no puede verla. */
  abstract ver(id: number): Observable<Capa>;

  /** Lo que el servidor WMS de la capa tiene en el punto; vacío si nada. Falla con ServidorDeMapasNoDisponible si no
   * responde. */
  abstract consultarPunto(punto: PuntoWms): Observable<ElementoDeServicio[]>;
}
