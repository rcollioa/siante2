import { Observable } from 'rxjs';
import { PuntoWms } from '../../../../mapa/domain/mapa';
import {
  Capa,
  CapaCatalogo,
  CapaPropia,
  EdicionCapa,
  ElementoDeServicio,
  NuevaCapa,
  ResumenCapa,
  SeccionCatalogo,
  VisibilidadEditable
} from '../../../domain/capa';

/** Las capas guardadas en SIATE. */
export abstract class CapasRepositorio {
  abstract listarMias(): Observable<CapaPropia[]>;

  /** Favoritas y formales elegidas por el usuario, que todavía puede ver. */
  abstract listarFavoritas(): Observable<ResumenCapa[]>;

  abstract catalogo(seccion: SeccionCatalogo): Observable<CapaCatalogo[]>;

  /** Falla con CapaNoEncontrada si no existe o el usuario no puede verla. */
  abstract ver(id: number): Observable<Capa>;

  /** Lo que el servidor WMS de la capa tiene en el punto; vacío si nada. Falla con ServidorDeMapasNoDisponible si no
   * responde. */
  abstract consultarPunto(punto: PuntoWms): Observable<ElementoDeServicio[]>;

  /** Crea una capa privada y sin elementos; entrega la capa con su ID. */
  abstract crear(nueva: NuevaCapa): Observable<CapaPropia>;

  /** Falla con CapaNoEncontrada si no existe o no es del usuario. */
  abstract editar(id: number, edicion: EdicionCapa): Observable<void>;

  /** Publica o despublica. Falla con CapaNoEncontrada si no existe o no es del usuario. */
  abstract cambiarVisibilidad(id: number, visibilidad: VisibilidadEditable): Observable<void>;

  /** Falla con CapaNoEncontrada si no existe o no es del usuario. */
  abstract eliminar(id: number): Observable<void>;
}
