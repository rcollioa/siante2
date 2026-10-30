package siante2.capas.adapter.out.wms;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import siante2.capas.application.port.out.ServiciosMapaPort;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.ElementoDeServicio;
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
    /** Una por clic y capa abierta: muchas menos que las imágenes. */
    private static final int CONSULTAS_A_LA_VEZ = 8;

    private final ServidoresMapa servidores;
    /** URL de la capa → formato en que se le pide GetFeatureInfo ("" si ninguno sirve). Se llena al leer su
     * GetCapabilities; solo crece con los servidores de capas registradas. */
    private final Map<String, String> formatosInfo = new ConcurrentHashMap<>();

    ClienteWms(ServidoresMapa servidores) {
        this.servidores = servidores;
    }

    @Override
    @Cacheable(cacheNames = "capacidades-wms", sync = true)
    public List<CapaDeServicio> capasWms(String url) {
        return UrlWms.desdeGetMap(url).orElseGet(() -> pedirCapacidades(url));
    }

    private List<CapaDeServicio> pedirCapacidades(String url) {
        CapacidadesWms.Leidas leidas = servidores.capacidades(UrlWms.capacidades(url), CapacidadesWms::leer);
        formatosInfo.put(url, InfoWms.formatoPreferido(leidas.formatosInfo()).orElse(""));
        if (leidas.capas().isEmpty()) {
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no publica capas.");
        }
        return leidas.capas();
    }

    @Override
    @ConcurrencyLimit(IMAGENES_A_LA_VEZ)
    public ImagenMapa mapaWms(String url, PedidoMapa pedido) {
        return servidores.imagen(UrlWms.mapa(url, pedido));
    }

    /** Si el servidor no permite consultar, o responde con un error o algo que no se puede leer, no hay nada que
     * mostrar en ese punto: el usuario igual ve el mapa. */
    @Override
    @ConcurrencyLimit(CONSULTAS_A_LA_VEZ)
    public List<ElementoDeServicio> infoWms(String url, ConsultaPunto consulta) {
        if (!formatosInfo.containsKey(url)) {
            pedirCapacidades(url);
        }
        String formato = formatosInfo.getOrDefault(url, "");
        if (formato.isEmpty()) {
            return List.of();
        }
        URI uri = UrlWms.info(url, consulta, formato);
        byte[] cuerpo;
        try {
            cuerpo = servidores.pedir(uri).getBody();
        } catch (RestClientException e) {
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no responde.", e);
        }
        try {
            return InfoWms.leer(cuerpo, formato);
        } catch (IllegalArgumentException e) {
            log.warn("GetFeatureInfo sin respuesta válida de {}: {}", uri.getHost(), e.getMessage());
            return List.of();
        }
    }
}
