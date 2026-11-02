package siante2.capas.adapter.out.oracle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.compartido.domain.Geometria;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lee el JSON con que SIATE guarda cada elemento en {@code LAYERS_FEATURES.JSON_METADATA}: un Feature con la
 * geometría en EPSG:4326 (los círculos con {@code center} y {@code radius}), el estilo en {@code properties} y el
 * nombre, la descripción y los metadatos en {@code metadata} (ver {@code convertOlToJsonMetadata} en SIATE). */
final class ElementoSiate {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Map<String, String> TIPOS = Map.of(
            "point", "Point", "linestring", "LineString", "polygon", "Polygon", "multipoint", "MultiPoint",
            "multilinestring", "MultiLineString", "multipolygon", "MultiPolygon");

    private ElementoSiate() {
    }

    /** Vacío si el JSON no tiene una geometría que se pueda dibujar. */
    static Optional<ElementoCapa> leer(int id, String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        try {
            return deFeature(id, JSON.readValue(texto, FeatureSiate.class));
        } catch (JacksonException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Los elementos en {@code LAYERS.JSON_METADATA}, donde SIATE guarda la capa completa: una colección, un arreglo
     * o un Feature. Numerados por posición; vacío en los que no se pueden dibujar y lista vacía si no es JSON. */
    static List<Optional<ElementoCapa>> leerColeccion(String texto) {
        List<JsonNode> features = new ArrayList<>();
        try {
            reunir(JSON.readTree(texto), features);
        } catch (JacksonException e) {
            return List.of();
        }
        List<Optional<ElementoCapa>> leidos = new ArrayList<>();
        for (int id = 0; id < features.size(); id++) {
            leidos.add(leer(id, features.get(id).toString()));
        }
        return leidos;
    }

    private static void reunir(JsonNode nodo, List<JsonNode> features) {
        if (nodo.isArray()) {
            nodo.forEach(hijo -> reunir(hijo, features));
        } else if (nodo.has("features")) {
            reunir(nodo.get("features"), features);
        } else if (nodo.isObject()) {
            features.add(nodo);
        }
    }

    private static Optional<ElementoCapa> deFeature(int id, FeatureSiate feature) {
        return geometria(feature.geometry()).map(geometria -> {
            MetadataSiate metadata = Objects.requireNonNullElse(feature.metadata(), MetadataSiate.VACIA);
            return new ElementoCapa(id, geometria, radio(feature.geometry()), texto(metadata.name()),
                    texto(metadata.description()), estilo(feature.properties()), metadatos(metadata));
        });
    }

    private static Optional<Geometria> geometria(GeometriaSiate g) {
        if (g == null || g.type() == null) {
            return Optional.empty();
        }
        if ("circle".equalsIgnoreCase(g.type())) {
            return g.center() == null || g.center().size() < 2 || g.radius() == null
                    ? Optional.empty()
                    : Optional.of(Geometria.simple("Point", (List<?>) coordenadas(g.center().subList(0, 2))));
        }
        String tipo = TIPOS.get(g.type().toLowerCase(Locale.ROOT));
        if (tipo == null || !(coordenadas(g.coordinates()) instanceof List<?> lista) || lista.isEmpty()
                || "Point".equals(tipo) && (lista.size() < 2 || !(lista.getFirst() instanceof Double))) {
            return Optional.empty();
        }
        return Optional.of(Geometria.simple(tipo, lista));
    }

    private static double radio(GeometriaSiate g) {
        return "circle".equalsIgnoreCase(g.type()) ? g.radius() : 0;
    }

    /** Números a Double en cualquier nivel de anidamiento, como espera {@link Geometria}. */
    private static Object coordenadas(Object valor) {
        if (valor instanceof Number numero) {
            return numero.doubleValue();
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().map(ElementoSiate::coordenadas).toList();
        }
        throw new IllegalArgumentException("Coordenada inválida: " + valor);
    }

    private static EstiloElemento estilo(Map<String, Object> propiedades) {
        if (propiedades == null) {
            return EstiloElemento.SIN_ESTILO;
        }
        return new EstiloElemento(color(propiedades.get("color")),
                propiedades.get("width") instanceof Number grosor ? grosor.doubleValue() : 0,
                color(propiedades.get("fill")),
                propiedades.get("image") instanceof String icono ? icono.strip() : "");
    }

    /** OpenLayers entrega los colores como texto CSS o como [r, g, b, a]. */
    private static String color(Object valor) {
        if (valor instanceof String texto) {
            return texto.strip();
        }
        if (valor instanceof List<?> partes && (partes.size() == 3 || partes.size() == 4)
                && partes.stream().allMatch(Number.class::isInstance)) {
            String numeros = partes.stream().map(ElementoSiate::numero).collect(Collectors.joining(", "));
            return (partes.size() == 4 ? "rgba(" : "rgb(") + numeros + ")";
        }
        return "";
    }

    private static String numero(Object valor) {
        double numero = ((Number) valor).doubleValue();
        return numero == Math.rint(numero) ? String.valueOf((long) numero) : String.valueOf(numero);
    }

    private static List<Metadato> metadatos(MetadataSiate metadata) {
        return metadata.metadata() == null ? List.of() : metadata.metadata().stream()
                .filter(m -> m != null && m.name() != null && !m.name().isBlank())
                .map(m -> new Metadato(m.name().strip(), texto(m.description())))
                .toList();
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).strip();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeatureSiate(GeometriaSiate geometry, Map<String, Object> properties, MetadataSiate metadata) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeometriaSiate(String type, Object coordinates, List<Object> center, Double radius) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MetadataSiate(Object name, Object description, List<MetadatoSiate> metadata) {

        static final MetadataSiate VACIA = new MetadataSiate(null, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MetadatoSiate(String name, Object description) {
    }
}
