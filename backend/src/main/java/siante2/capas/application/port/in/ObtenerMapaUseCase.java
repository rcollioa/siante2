package siante2.capas.application.port.in;

import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMapa;

public interface ObtenerMapaUseCase {

    /** Una imagen del servidor WMS de la capa, si el usuario puede verla y pide capas que ese servidor publica.
     * Lanza {@code RecursoNoEncontradoException} si la capa no existe, no es WMS o no puede verla. */
    ImagenMapa obtenerMapa(int capaId, String ivUser, PedidoMapa pedido);
}
