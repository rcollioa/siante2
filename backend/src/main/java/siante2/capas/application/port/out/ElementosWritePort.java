package siante2.capas.application.port.out;

import java.util.List;
import java.util.Optional;
import siante2.capas.domain.CambiosElementos;

/** Los elementos de una capa, que SIATE también escribe. */
public interface ElementosWritePort {

    /** Todo o nada. Devuelve los ID de los nuevos, en el mismo orden, o vacío si la capa no existe, no es del dueño o no
     * se puede dibujar en ella. Lanza {@code SolicitudInvalidaException} si una geometría no es válida o si un elemento
     * modificado ya no existe. */
    Optional<List<Integer>> guardar(int capaId, int duenoId, CambiosElementos cambios);
}
