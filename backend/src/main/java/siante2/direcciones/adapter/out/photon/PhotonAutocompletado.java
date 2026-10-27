package siante2.direcciones.adapter.out.photon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.direcciones.application.port.out.AutocompletadoPort;
import siante2.direcciones.domain.Direccion;
import siante2.direcciones.domain.ZonaVisible;

/** Photon (OpenStreetMap) está hecho para autocompletar. Sin reintento: si una sugerencia falla, la siguiente
 * tecla la vuelve a pedir y Enter sigue buscando con Nominatim. La caché evita repetir lo ya escrito. */
@Component
class PhotonAutocompletado implements AutocompletadoPort {

    static final int MAXIMO_SUGERENCIAS = 5;
    /** Se piden más de las que se muestran: los repetidos y los de otros países se descartan. */
    static final int PEDIDAS = 10;
    /** Con más zoom, el radio de Photon es tan chico que sin coincidencias cerca vuelve al orden mundial (cerca de
     * la frontera, Argentina). Más cerca que una ciudad no mejora el orden. */
    static final int ZOOM_MAXIMO_REFERENCIA = 12;

    private final RestClient cliente;
    private final Optional<String> recuadro;
    private final Set<String> paises;

    PhotonAutocompletado(@Qualifier("photonRestClient") RestClient cliente,
            @Value("${app.autocompletado.recuadro:}") String recuadro,
            @Value("${app.autocompletado.paises:}") String paises) {
        this.cliente = cliente;
        this.recuadro = Optional.of(recuadro).filter(r -> !r.isBlank());
        this.paises = Arrays.stream(paises.split(","))
                .map(pais -> pais.strip().toUpperCase(Locale.ROOT))
                .filter(pais -> !pais.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    @Cacheable("sugerencias")
    public List<Direccion> sugerir(String texto, Optional<ZonaVisible> zona) {
        try {
            RespuestaPhoton respuesta = cliente.get()
                    .uri(uri -> uri.path("/api")
                            .queryParam("q", texto)
                            .queryParam("limit", PEDIDAS)
                            .queryParamIfPresent("bbox", recuadro)
                            .queryParamIfPresent("lat", zona.map(ZonaVisible::latitud))
                            .queryParamIfPresent("lon", zona.map(ZonaVisible::longitud))
                            .queryParamIfPresent("zoom", zona.map(z -> Math.min(z.zoom(), ZOOM_MAXIMO_REFERENCIA)))
                            .build())
                    .retrieve()
                    .body(RespuestaPhoton.class);
            if (respuesta == null || respuesta.features() == null) {
                return List.of();
            }
            // El recuadro también toma parte de Argentina. OpenStreetMap tiene varios objetos con el mismo nombre
            // (tramos de una calle, una plaza como lugar y como calle): queda el primero, el más relevante.
            return respuesta.features().stream()
                    .filter(lugar -> paises.isEmpty() || lugar.esDe(paises))
                    .map(Lugar::aDireccion)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toMap(d -> sinTildes(d.nombre()), d -> d, (primera, repetida) -> primera,
                            LinkedHashMap::new))
                    .values().stream()
                    .limit(MAXIMO_SUGERENCIAS)
                    .toList();
        } catch (RestClientException e) {
            throw new ServicioExternoNoDisponibleException("El servicio de sugerencias de direcciones no respondió.", e);
        }
    }

    /** "San Martin" y "San Martín" son el mismo nombre escrito de dos formas en OpenStreetMap. */
    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaPhoton(List<Lugar> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Lugar(Geometria geometry, Propiedades properties) {

        boolean esDe(Set<String> paises) {
            return properties != null && properties.countrycode() != null
                    && paises.contains(properties.countrycode().toUpperCase(Locale.ROOT));
        }

        /** Photon no trae un nombre completo: se arma con nombre, calle y número, ciudad y región, sin repetir. Si
         * el nombre y la calle se contienen ("Plaza de Armas" en "Plaza de Armas de Santiago"), queda el más largo. */
        Optional<Direccion> aDireccion() {
            if (geometry == null || geometry.coordinates() == null || geometry.coordinates().size() < 2
                    || properties == null) {
                return Optional.empty();
            }
            Propiedades p = properties;
            String propio = p.name();
            String calle = Stream.of(p.street(), p.housenumber()).filter(Objects::nonNull).collect(Collectors.joining(" "));
            if (propio != null && !calle.isEmpty()) {
                if (contiene(calle, propio)) {
                    propio = null;
                } else if (contiene(propio, calle)) {
                    calle = "";
                }
            }
            String ciudad = Stream.of(p.city(), p.district(), p.county()).filter(Objects::nonNull).findFirst().orElse(null);
            String nombre = Stream.of(propio, calle, ciudad, p.state())
                    .filter(parte -> parte != null && !parte.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new))
                    .stream().collect(Collectors.joining(", "));
            if (nombre.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Direccion(nombre, geometry.coordinates().get(1), geometry.coordinates().get(0)));
        }

        private static boolean contiene(String texto, String parte) {
            return texto.toLowerCase(Locale.ROOT).contains(parte.toLowerCase(Locale.ROOT));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Geometria(List<Double> coordinates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Propiedades(String name, String street, String housenumber, String city, String district, String county,
            String state, String countrycode) {
    }
}
