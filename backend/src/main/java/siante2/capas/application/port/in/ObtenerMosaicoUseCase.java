package siante2.capas.application.port.in;

import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMosaico;

public interface ObtenerMosaicoUseCase {

    /** Un mosaico del servidor WMTS de la capa, si el usuario puede verla y pide una capa, matriz, fila y columna que
     * ese servidor publica. Lanza {@code RecursoNoEncontradoException} si la capa no existe, no es WMTS o no puede
     * verla. */
    ImagenMapa obtenerMosaico(int capaId, String ivUser, PedidoMosaico pedido);
}
