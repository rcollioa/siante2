package siante2.capas.application.port.in;

import java.util.List;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.ElementoDeServicio;

public interface ConsultarPuntoUseCase {

    /** Lo que el servidor WMS de la capa tiene en un punto, si el usuario puede verla y consulta capas que ese
     * servidor publica. Vacío si no hay nada o el servidor no permite consultar. Lanza
     * {@code RecursoNoEncontradoException} si la capa no existe, no es WMS o no puede verla. */
    List<ElementoDeServicio> consultarPunto(int capaId, String ivUser, ConsultaPunto consulta);
}
