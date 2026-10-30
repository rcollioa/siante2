package siante2.capas.application.port.out;

import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMosaico;
import siante2.capas.domain.ServicioMapa;

/** Los servidores WMTS externos de las capas por URL. Si no responden, lanza
 * {@code ServicioExternoNoDisponibleException}. */
public interface ServiciosWmtsPort {

    /** Las capas que publica el servidor y la grilla de mosaicos con que el mapa las dibuja, de su GetCapabilities. */
    ServicioMapa servicioWmts(String url);

    /** Un mosaico de una capa publicada, en una matriz de la grilla del servicio. */
    ImagenMapa mosaicoWmts(String url, PedidoMosaico pedido);
}
