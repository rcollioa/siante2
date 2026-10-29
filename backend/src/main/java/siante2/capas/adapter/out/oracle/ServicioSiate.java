package siante2.capas.adapter.out.oracle;

import java.util.Locale;
import java.util.Optional;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.TipoServicio;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lee el servidor de una capa por URL en {@code LAYERS.JSON_METADATA}: SIATE guarda {@code {"type":"WMS","url":...}},
 * a veces dentro de un arreglo ({@code loadLayer} en SIATE toma el primero). */
final class ServicioSiate {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ServicioSiate() {
    }

    /** Vacío si el JSON no es de un servidor de mapas conocido o no trae una URL http(s). */
    static Optional<OrigenServicio> leer(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode raiz = JSON.readTree(texto);
            JsonNode servicio = raiz.isArray() && !raiz.isEmpty() ? raiz.get(0) : raiz;
            return tipo(servicio.path("type").asString("")).flatMap(tipo -> {
                String url = servicio.path("url").asString("").trim();
                String minuscula = url.toLowerCase(Locale.ROOT);
                return minuscula.startsWith("http://") || minuscula.startsWith("https://")
                        ? Optional.of(new OrigenServicio(tipo, url))
                        : Optional.empty();
            });
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    private static Optional<TipoServicio> tipo(String texto) {
        try {
            return Optional.of(TipoServicio.valueOf(texto.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
