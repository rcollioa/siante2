package siante2.capas.adapter.out.wms;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
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
    /** Lo habitual es http → https y www → sin www (geoportal.cl hace ambos). */
    private static final int MAXIMO_REDIRECCIONES = 3;

    private final RestClient cliente;
    /** Servidor pedido → servidor al que redirigió ("https://www.x.cl" → "https://x.cl"), para que cada mosaico no
     * repita las redirecciones. Solo crece con los servidores de capas registradas. */
    private final Map<String, String> redirigidos = new ConcurrentHashMap<>();
    /** URL de la capa → formato en que se le pide GetFeatureInfo ("" si ninguno sirve). Se llena al leer su
     * GetCapabilities; como {@code redirigidos}, solo con servidores de capas registradas. */
    private final Map<String, String> formatosInfo = new ConcurrentHashMap<>();

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
        List<CapaDeServicio> capas;
        try {
            CapacidadesWms.Leidas leidas = CapacidadesWms.leer(pedir(uri).getBody());
            capas = leidas.capas();
            formatosInfo.put(url, InfoWms.formatoPreferido(leidas.formatosInfo()).orElse(""));
        } catch (HttpClientErrorException e) {
            // El servidor responde, pero no tiene el servicio (p. ej. 404: lo dieron de baja). Sin el HTML en el log.
            log.warn("GetCapabilities de {} respondió {}", uri.getHost(), e.getStatusCode());
            throw new ServicioExternoNoDisponibleException(
                    "El servidor de mapas ya no publica el servicio de la capa (respondió " + e.getStatusCode().value() + ").", e);
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
        ResponseEntity<byte[]> respuesta;
        try {
            respuesta = pedir(UrlWms.mapa(url, pedido));
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
            cuerpo = pedir(uri).getBody();
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

    /** GET que sigue las redirecciones validando cada destino con las mismas reglas que la URL original: el cliente
     * HTTP no las sigue solo, para que un servidor no lleve al backend a una dirección no permitida. */
    private ResponseEntity<byte[]> pedir(URI pedida) {
        URI uri = conRedireccionConocida(pedida);
        for (int saltos = 0; ; saltos++) {
            UrlWms.permitida(uri);
            ResponseEntity<byte[]> respuesta = cliente.get().uri(uri).retrieve().toEntity(byte[].class);
            URI destino = respuesta.getHeaders().getLocation();
            if (!respuesta.getStatusCode().is3xxRedirection() || destino == null) {
                return respuesta;
            }
            if (saltos == MAXIMO_REDIRECCIONES) {
                throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa redirige demasiadas veces.");
            }
            URI siguiente = uri.resolve(destino);
            redirigidos.put(origen(uri), origen(siguiente));
            uri = siguiente;
        }
    }

    /** Si este servidor ya redirigió a otro, se le pide directo al último destino, con la misma ruta y consulta. */
    private URI conRedireccionConocida(URI uri) {
        String pedido = origen(uri);
        String destino = pedido;
        for (int saltos = 0; redirigidos.containsKey(destino) && saltos < MAXIMO_REDIRECCIONES; saltos++) {
            destino = redirigidos.get(destino);
        }
        return destino.equals(pedido) ? uri : URI.create(destino + uri.toString().substring(pedido.length()));
    }

    private static String origen(URI uri) {
        return uri.getScheme() + "://" + uri.getRawAuthority();
    }
}
