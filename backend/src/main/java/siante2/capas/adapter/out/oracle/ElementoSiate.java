package siante2.capas.adapter.out.oracle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.SolicitudInvalidaException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/** Lee y escribe el JSON con que SIATE guarda cada elemento en {@code LAYERS_FEATURES.JSON_METADATA}: un Feature con la
 * geometría en EPSG:4326 (los círculos con {@code center} y {@code radius}), el estilo en {@code properties} y el
 * nombre, la descripción y los metadatos en {@code metadata} (ver {@code convertOlToJsonMetadata} en SIATE). */
final class ElementoSiate {

    private static final Logger log = LoggerFactory.getLogger(ElementoSiate.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    /** El formato de {@code creationdate} de SIATE ({@code dateAsDDMMYYYYHHNNSS}). */
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    /** El de EPSG:3857. */
    private static final double RADIO_TERRESTRE = 6378137;
    private static final int LADOS_DEL_CIRCULO = 64;
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
        List<String> features = featuresDe(texto);
        List<Optional<ElementoCapa>> leidos = new ArrayList<>();
        for (int id = 0; id < features.size(); id++) {
            leidos.add(leer(id, features.get(id)));
        }
        return leidos;
    }

    /** El JSON de cada elemento de una colección, en su posición. */
    static List<String> featuresDe(String texto) {
        List<JsonNode> features = new ArrayList<>();
        try {
            reunir(JSON.readTree(texto), features);
        } catch (JacksonException e) {
            return List.of();
        }
        return features.stream().map(JsonNode::toString).toList();
    }

    /** El JSON de un elemento, como lo escribe {@code convertOlToJsonMetadata} de SIATE. Sobre el que ya tenía
     * ({@code actual}), para no perder lo que aquí no se edita: el ícono, la leyenda de una ruta o la fecha de creación. */
    static String escribir(int capaId, int id, ElementoCapa elemento, String actual, LocalDateTime ahora) {
        ObjectNode feature = comoObjeto(actual);
        if (feature.isEmpty()) {
            // El ID que SIATE le da al dibujarlo; al leerlo calcula otro, con ID_FEATURE.
            feature.put("id", capaId + "-" + (capaId * 100000L) + id);
            feature.put("type", "Feature");
        }
        feature.set("geometry", geometriaDe(elemento, feature.get("geometry"), ahora));
        estiloEn(hijo(feature, "properties"), elemento.estilo());
        metadataEn(hijo(feature, "metadata"), elemento);
        return feature.toString();
    }

    /** El JSON que ya tenía el elemento, o un objeto vacío si no hay o está dañado. */
    private static ObjectNode comoObjeto(String texto) {
        if (texto != null) {
            try {
                if (JSON.readTree(texto) instanceof ObjectNode objeto) {
                    return objeto;
                }
            } catch (JacksonException e) {
                log.warn("Se reemplaza el JSON dañado de un elemento");
            }
        }
        return JSON.createObjectNode();
    }

    private static ObjectNode hijo(ObjectNode padre, String nombre) {
        return padre.get(nombre) instanceof ObjectNode hijo ? hijo : padre.putObject(nombre);
    }

    private static ObjectNode geometriaDe(ElementoCapa elemento, JsonNode anterior, LocalDateTime ahora) {
        ObjectNode geometria = JSON.createObjectNode();
        if (elemento.radio() > 0) {
            geometria.put("type", "Circle");
            geometria.set("center", JSON.valueToTree(elemento.geometria().coordinates()));
            geometria.put("radius", elemento.radio());
        } else {
            geometria.put("type", elemento.geometria().type());
            geometria.set("coordinates", JSON.valueToTree(elemento.geometria().coordinates()));
        }
        JsonNode fecha = anterior == null ? null : anterior.get("creationdate");
        geometria.put("creationdate", fecha != null && fecha.isString() ? fecha.asString() : FECHA.format(ahora));
        return geometria;
    }

    /** Sin tocar {@code image}: el ícono no se edita todavía. */
    private static void estiloEn(ObjectNode propiedades, EstiloElemento estilo) {
        EstiloElemento dado = Objects.requireNonNullElse(estilo, EstiloElemento.SIN_ESTILO);
        ponerOQuitar(propiedades, "color", dado.color());
        ponerOQuitar(propiedades, "fill", dado.relleno());
        if (dado.grosor() > 0) {
            propiedades.put("width", dado.grosor());
        } else {
            propiedades.remove("width");
        }
    }

    private static void ponerOQuitar(ObjectNode nodo, String nombre, String valor) {
        if (valor == null || valor.isEmpty()) {
            nodo.remove(nombre);
        } else {
            nodo.put(nombre, valor);
        }
    }

    private static void metadataEn(ObjectNode metadata, ElementoCapa elemento) {
        metadata.put("name", Objects.requireNonNullElse(elemento.nombre(), ""));
        metadata.put("description", Objects.requireNonNullElse(elemento.descripcion(), ""));
        ArrayNode datos = metadata.putArray("metadata");
        if (elemento.metadatos() != null) {
            elemento.metadatos().stream()
                    .filter(dato -> dato.nombre() != null && !dato.nombre().isBlank())
                    .forEach(dato -> datos.addObject().put("name", dato.nombre())
                            .put("description", Objects.requireNonNullElse(dato.valor(), "")));
        }
    }

    /** La columna {@code TYPE} como la escribe {@code LayerService.setFeature} de SIATE. */
    static String tipo(ElementoCapa elemento) {
        if (elemento.radio() > 0) {
            return "Circle";
        }
        String tipo = elemento.geometria().type();
        return switch (tipo) {
            case "MultiLineString", "MultiPolygon" -> "Collection";
            case "MultiPoint" -> elemento.geometria().coordinates().size() == 1 ? "Point" : "Collection";
            default -> tipo;
        };
    }

    /** Lo que va en {@code SPATIAL_OBJECT}, para el análisis espacial de SIATE. Un círculo va como polígono: WKT no tiene
     * círculos, y SIATE guardaba el radio en metros o kilómetros como si fueran grados. */
    static Geometria geometriaEspacial(ElementoCapa elemento) {
        return elemento.radio() > 0
                ? circuloComoPoligono(elemento.geometria(), elemento.radio())
                : Geometria.simple(elemento.geometria().type(), sinAltura(elemento.geometria().coordinates()));
    }

    /** Solo longitud y latitud, como {@code extractCoordinates} de SIATE: un KML o un GeoJSON suelen traer la altura. */
    private static List<?> sinAltura(List<?> coordenadas) {
        if (coordenadas == null || coordenadas.isEmpty()) {
            return coordenadas;
        }
        if (coordenadas.getFirst() instanceof Number) {
            return coordenadas.size() > 2 ? coordenadas.subList(0, 2) : coordenadas;
        }
        return coordenadas.stream().map(hijo -> hijo instanceof List<?> lista ? sinAltura(lista) : hijo).toList();
    }

    /** Se calcula en EPSG:3857, donde está el radio, para que coincida con el círculo que se ve en el mapa. */
    private static Geometria circuloComoPoligono(Geometria centro, double radio) {
        List<?> grados = centro.coordinates();
        if (grados.size() < 2 || !(grados.get(0) instanceof Number longitud) || !(grados.get(1) instanceof Number latitud)) {
            throw new SolicitudInvalidaException("Cada punto debe tener longitud y latitud numéricas.");
        }
        double x = Math.toRadians(longitud.doubleValue()) * RADIO_TERRESTRE;
        double y = Math.log(Math.tan(Math.PI / 4 + Math.toRadians(latitud.doubleValue()) / 2)) * RADIO_TERRESTRE;
        List<List<Double>> anillo = new ArrayList<>();
        for (int lado = 0; lado < LADOS_DEL_CIRCULO; lado++) {
            double angulo = 2 * Math.PI * lado / LADOS_DEL_CIRCULO;
            anillo.add(aGrados(x + radio * Math.cos(angulo), y + radio * Math.sin(angulo)));
        }
        anillo.add(anillo.getFirst());
        return Geometria.simple("Polygon", List.of(anillo));
    }

    private static List<Double> aGrados(double x, double y) {
        return List.of(Math.toDegrees(x / RADIO_TERRESTRE),
                Math.toDegrees(2 * Math.atan(Math.exp(y / RADIO_TERRESTRE)) - Math.PI / 2));
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
