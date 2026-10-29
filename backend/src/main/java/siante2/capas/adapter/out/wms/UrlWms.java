package siante2.capas.adapter.out.wms;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.Extension;
import siante2.capas.domain.PedidoMapa;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

/** Arma las URL de un servidor WMS a partir de la que guardó SIATE (de GetCapabilities o de GetMap). Conserva los
 * parámetros propios del servidor (p. ej. {@code map=} de MapServer) y reemplaza los de WMS. */
final class UrlWms {

    private static final Set<String> DE_WMS = Set.of("service", "request", "version", "layers", "bbox", "width",
            "height", "format", "crs", "srs", "styles", "transparent", "tiled", "exceptions");

    private UrlWms() {
    }

    static URI capacidades(String url) {
        return armar(url, parametros("SERVICE", "WMS", "REQUEST", "GetCapabilities"));
    }

    static URI mapa(String url, PedidoMapa pedido) {
        Map<String, String> wms = parametros("SERVICE", "WMS", "REQUEST", "GetMap", "VERSION", pedido.version(),
                "LAYERS", String.join(",", pedido.capas()), "STYLES", "",
                "1.3.0".equals(pedido.version()) ? "CRS" : "SRS", pedido.crs(), "BBOX", pedido.bbox(),
                "WIDTH", String.valueOf(pedido.ancho()), "HEIGHT", String.valueOf(pedido.alto()),
                "FORMAT", pedido.formato(), "TRANSPARENT", String.valueOf(pedido.transparente()).toUpperCase(Locale.ROOT));
        return armar(url, wms);
    }

    /** Si SIATE guardó una URL GetMap, sus capas van en {@code layers}: no hace falta preguntar al servidor. */
    static Optional<List<CapaDeServicio>> desdeGetMap(String url) {
        Map<String, String> consulta = consulta(url);
        String capas = consulta.getOrDefault("layers", "");
        if (!"getmap".equalsIgnoreCase(consulta.getOrDefault("request", "")) || capas.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Arrays.stream(capas.split(",")).map(String::trim).filter(c -> !c.isEmpty())
                .map(c -> new CapaDeServicio(c, c, (Extension) null)).toList());
    }

    /** Solo http(s) y nunca a la propia máquina ni a direcciones de enlace local (metadatos de la nube): una capa
     * la registra cualquier usuario y la URL no debe llevar al backend a donde no corresponde. */
    static void permitida(URI uri) {
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!esquema.equals("http") && !esquema.equals("https") || uri.getHost() == null) {
            throw new ServicioExternoNoDisponibleException("La dirección del servidor de mapas no es válida.");
        }
        try {
            for (InetAddress direccion : InetAddress.getAllByName(uri.getHost())) {
                if (direccion.isLoopbackAddress() || direccion.isLinkLocalAddress() || direccion.isAnyLocalAddress()) {
                    throw new ServicioExternoNoDisponibleException("La dirección del servidor de mapas no está permitida.");
                }
            }
        } catch (UnknownHostException e) {
            throw new ServicioExternoNoDisponibleException("No se encontró el servidor de mapas de la capa.", e);
        }
    }

    private static URI armar(String url, Map<String, String> wms) {
        int inicioConsulta = url.indexOf('?');
        String base = inicioConsulta < 0 ? url : url.substring(0, inicioConsulta);
        List<String> pares = new ArrayList<>();
        if (inicioConsulta >= 0) {
            for (String par : url.substring(inicioConsulta + 1).split("&")) {
                String clave = par.split("=", 2)[0];
                if (!par.isBlank() && !DE_WMS.contains(clave.toLowerCase(Locale.ROOT))) {
                    pares.add(par);
                }
            }
        }
        wms.forEach((clave, valor) -> pares.add(clave + "=" + codificar(valor)));
        return URI.create(base + "?" + String.join("&", pares));
    }

    private static Map<String, String> consulta(String url) {
        Map<String, String> valores = new LinkedHashMap<>();
        int inicio = url.indexOf('?');
        if (inicio >= 0) {
            for (String par : url.substring(inicio + 1).split("&")) {
                String[] partes = par.split("=", 2);
                valores.put(partes[0].toLowerCase(Locale.ROOT),
                        partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8) : "");
            }
        }
        return valores;
    }

    private static Map<String, String> parametros(String... claveValor) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < claveValor.length; i += 2) {
            mapa.put(claveValor[i], claveValor[i + 1]);
        }
        return mapa;
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
