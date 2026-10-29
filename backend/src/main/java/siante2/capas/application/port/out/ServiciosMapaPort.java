package siante2.capas.application.port.out;

import java.util.List;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMapa;

/** Los servidores de mapas externos de las capas por URL. Si no responden, lanza
 * {@code ServicioExternoNoDisponibleException}. */
public interface ServiciosMapaPort {

    /** Las capas que publica un servidor WMS, de su GetCapabilities (o de la URL GetMap que guardó SIATE). */
    List<CapaDeServicio> capasWms(String url);

    ImagenMapa mapaWms(String url, PedidoMapa pedido);
}
