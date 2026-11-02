package siante2.capas.adapter.out.wms;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import siante2.capas.domain.PedidoMosaico;

/** Arma las URL de un servidor WMTS a partir de la que guardó SIATE: la del GetCapabilities (a menudo un
 * {@code WMTSCapabilities.xml}) o la del servicio. */
final class UrlWmts {

    private static final Set<String> DE_WMTS = Set.of("service", "request", "version", "layer", "style", "format",
            "tilematrixset", "tilematrix", "tilerow", "tilecol");
    private static final Pattern VARIABLE = Pattern.compile("\\{(\\w+)}");

    private UrlWmts() {
    }

    /** Un archivo XML o una URL que ya pide GetCapabilities se usan tal cual; a las demás se les agrega KVP. */
    static URI capacidades(String url) {
        String minuscula = url.toLowerCase(Locale.ROOT);
        String ruta = minuscula.split("\\?", 2)[0];
        if (ruta.endsWith(".xml") || minuscula.contains("request=getcapabilities")) {
            return URI.create(url);
        }
        // Con orden fijo: el de Map.of cambia en cada arranque de la JVM, y con él la URL pedida.
        Map<String, String> wmts = new LinkedHashMap<>();
        wmts.put("SERVICE", "WMTS");
        wmts.put("REQUEST", "GetCapabilities");
        return armar(url, wmts);
    }

    /** Con la plantilla REST de la capa si el servidor la publica; si no, por KVP en la URL de GetTile (o en la del
     * GetCapabilities). Las rutas relativas se resuelven contra la URL de la capa. */
    static URI mosaico(String url, CapacidadesWmts.Leidas leidas, CapacidadesWmts.Capa capa, String grilla,
                       PedidoMosaico pedido) {
        URI base = URI.create(url);
        if (!capa.plantilla().isEmpty()) {
            Map<String, String> valores = Map.of("tilematrixset", grilla, "tilematrix", pedido.matriz(),
                    "tilerow", String.valueOf(pedido.fila()), "tilecol", String.valueOf(pedido.columna()),
                    "style", capa.estilo(), "layer", capa.id());
            Matcher variables = VARIABLE.matcher(capa.plantilla());
            StringBuilder armada = new StringBuilder();
            while (variables.find()) {
                String valor = valores.get(variables.group(1).toLowerCase(Locale.ROOT));
                variables.appendReplacement(armada, Matcher.quoteReplacement(valor == null ? variables.group() : codificar(valor)));
            }
            variables.appendTail(armada);
            return base.resolve(armada.toString());
        }
        Map<String, String> wmts = new LinkedHashMap<>();
        wmts.put("SERVICE", "WMTS");
        wmts.put("REQUEST", "GetTile");
        wmts.put("VERSION", "1.0.0");
        wmts.put("LAYER", capa.id());
        wmts.put("STYLE", capa.estilo());
        wmts.put("FORMAT", capa.formato());
        wmts.put("TILEMATRIXSET", grilla);
        wmts.put("TILEMATRIX", pedido.matriz());
        wmts.put("TILEROW", String.valueOf(pedido.fila()));
        wmts.put("TILECOL", String.valueOf(pedido.columna()));
        String getTile = leidas.urlGetTile().isEmpty() ? url : base.resolve(leidas.urlGetTile()).toString();
        return armar(getTile, wmts);
    }

    private static URI armar(String url, Map<String, String> wmts) {
        int inicioConsulta = url.indexOf('?');
        String base = inicioConsulta < 0 ? url : url.substring(0, inicioConsulta);
        List<String> pares = new ArrayList<>();
        if (inicioConsulta >= 0) {
            for (String par : url.substring(inicioConsulta + 1).split("&")) {
                if (!par.isBlank() && !DE_WMTS.contains(par.split("=", 2)[0].toLowerCase(Locale.ROOT))) {
                    pares.add(par);
                }
            }
        }
        wmts.forEach((clave, valor) -> pares.add(clave + "=" + codificar(valor)));
        return URI.create(base + "?" + String.join("&", pares));
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
