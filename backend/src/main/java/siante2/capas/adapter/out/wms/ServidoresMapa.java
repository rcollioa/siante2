package siante2.capas.adapter.out.wms;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import siante2.capas.domain.ImagenMapa;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

/** Los pedidos a los servidores de mapas de las capas por URL (WMS y WMTS): siguen las redirecciones validando cada
 * destino y traducen sus fallas a {@code ServicioExternoNoDisponibleException}. */
@Component
class ServidoresMapa {

    private static final Logger log = LoggerFactory.getLogger(ServidoresMapa.class);
    /** Lo habitual es http → https y www → sin www (geoportal.cl hace ambos). */
    private static final int MAXIMO_REDIRECCIONES = 3;

    private final RestClient cliente;
    /** Servidor pedido → servidor al que redirigió ("https://www.x.cl" → "https://x.cl"), para que cada mosaico no
     * repita las redirecciones. Solo crece con los servidores de capas registradas. */
    private final Map<String, String> redirigidos = new ConcurrentHashMap<>();

    ServidoresMapa(@Qualifier("serviciosMapaRestClient") RestClient cliente) {
        this.cliente = cliente;
    }

    /** Pide un GetCapabilities y lo lee con {@code leer}, que lanza {@code IllegalArgumentException} si no lo
     * reconoce. */
    <T> T capacidades(URI uri, Function<byte[], T> leer) {
        try {
            return leer.apply(pedir(uri).getBody());
        } catch (HttpClientErrorException e) {
            // El servidor responde, pero no tiene el servicio (p. ej. 404: lo dieron de baja). Sin el HTML en el log.
            log.warn("GetCapabilities de {} respondió {}", uri.getHost(), e.getStatusCode());
            throw new ServicioExternoNoDisponibleException(
                    "El servidor de mapas ya no publica el servicio de la capa (respondió " + e.getStatusCode().value() + ").", e);
        } catch (RestClientException | IllegalArgumentException e) {
            log.warn("GetCapabilities sin respuesta válida de {}: {}", uri.getHost(), e.getMessage());
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no responde.", e);
        }
    }

    /** Un servidor con error suele responder 200 con un XML (ServiceException): eso no es una imagen. */
    ImagenMapa imagen(URI uri) {
        ResponseEntity<byte[]> respuesta;
        try {
            respuesta = pedir(uri);
        } catch (RestClientException e) {
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no responde.", e);
        }
        MediaType tipo = respuesta.getHeaders().getContentType();
        if (tipo == null || !"image".equals(tipo.getType()) || respuesta.getBody() == null) {
            throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no entregó una imagen.");
        }
        return new ImagenMapa(respuesta.getBody(), tipo.getType() + "/" + tipo.getSubtype());
    }

    /** GET que sigue las redirecciones validando cada destino con las mismas reglas que la URL original: el cliente
     * HTTP no las sigue solo, para que un servidor no lleve al backend a una dirección no permitida. */
    ResponseEntity<byte[]> pedir(URI pedida) {
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
