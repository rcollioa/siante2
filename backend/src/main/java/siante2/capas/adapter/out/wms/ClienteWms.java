package siante2.capas.adapter.out.wms;

import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import siante2.capas.application.port.out.ServiciosMapaPort;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMapa;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

/** Habla con los servidores WMS de las capas por URL, como hacía SIATE desde su backend ({@code LayerHelper}). El
 * GetCapabilities queda en caché: SIATE lo pedía cada vez que se abría la capa. */
@Component
class ClienteWms implements ServiciosMapaPort {

    private static final Logger log = LoggerFactory.getLogger(ClienteWms.class);
    /** Imágenes a la vez por réplica: un mapa pide decenas de mosaicos y no deben agotar las conexiones a terceros. */
    private static final int IMAGENES_A_LA_VEZ = 32;

    private final RestClient cliente;

    ClienteWms(@Qualifier("serviciosMapaRestClient") RestClient cliente) {
        this.cliente = cliente;
    }

    @Override
    @Cacheable(cacheNames = "capacidades-wms", sync = true)
    public List<CapaDeServicio> capasWms(String url) {
        return UrlWms.desdeGetMap(url).orElseGet(() -> pedirCapacidades(url));
    }

    private List<CapaDeServicio> pedirCapacidades(String url) {
        URI uri = UrlWms.capacidades(url);
        UrlWms.permitida(uri);
        List<CapaDeServicio> capas;
        try {
            capas = CapacidadesWms.leer(cliente.get().uri(uri).retrieve().body(byte[].class));
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("GetCapabilities sin respuesta válida de {}: {}", uri.getHost(), e.getMessage());
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no responde.", e);
        }
        if (capas.isEmpty()) {
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no publica capas.");
        }
        return capas;
    }

    @Override
    @ConcurrencyLimit(IMAGENES_A_LA_VEZ)
    public ImagenMapa mapaWms(String url, PedidoMapa pedido) {
        URI uri = UrlWms.mapa(url, pedido);
        UrlWms.permitida(uri);
        ResponseEntity<byte[]> respuesta;
        try {
            respuesta = cliente.get().uri(uri).retrieve().toEntity(byte[].class);
        } catch (RestClientException e) {
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no responde.", e);
        }
        MediaType tipo = respuesta.getHeaders().getContentType();
        // Un WMS con error responde 200 con un XML (ServiceException): eso no es una imagen.
        if (tipo == null || !"image".equals(tipo.getType()) || respuesta.getBody() == null) {
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no entregó una imagen.");
        }
        return new ImagenMapa(respuesta.getBody(), tipo.getType() + "/" + tipo.getSubtype());
    }
}
