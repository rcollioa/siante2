package siante2.capas.domain;

import siante2.compartido.domain.SolicitudInvalidaException;

/** Qué hay en un punto de una capa WMS (GetFeatureInfo): la imagen en que el usuario hizo clic y el píxel dentro de
 * ella ({@code i} de izquierda a derecha, {@code j} de arriba abajo). */
public record ConsultaPunto(PedidoMapa mapa, int i, int j) {

    public ConsultaPunto {
        if (mapa == null || i < 0 || j < 0 || i >= mapa.ancho() || j >= mapa.alto()) {
            throw new SolicitudInvalidaException("El punto consultado no está dentro de la imagen.");
        }
    }
}
