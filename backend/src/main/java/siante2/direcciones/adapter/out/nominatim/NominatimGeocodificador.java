package siante2.direcciones.adapter.out.nominatim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.direcciones.application.port.out.GeocodificadorPort;
import siante2.direcciones.domain.Direccion;

/** Nominatim admite como máximo una solicitud por segundo y prohíbe autocompletar: el frontend busca
 * al confirmar, la caché evita repetir y {@code @ConcurrencyLimit} envía de a una. */
@Component
class NominatimGeocodificador implements GeocodificadorPort {

    static final int MAXIMO_RESULTADOS = 5;

    private final RestClient cliente;
    private final Optional<String> paises;

    NominatimGeocodificador(@Qualifier("nominatimRestClient") RestClient cliente,
            @Value("${app.geocodificacion.paises:}") String paises) {
        this.cliente = cliente;
        this.paises = Optional.of(paises).filter(p -> !p.isBlank());
    }

    @Override
    @Cacheable("direcciones")
    @Retryable(includes = ServicioExternoNoDisponibleException.class, maxRetries = 1, delay = 500)
    @ConcurrencyLimit(1)
    public List<Direccion> buscar(String texto) {
        try {
            ResultadoNominatim[] resultados = cliente.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", texto)
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", MAXIMO_RESULTADOS)
                            .queryParam("accept-language", "es")
                            .queryParamIfPresent("countrycodes", paises)
                            .build())
                    .retrieve()
                    .body(ResultadoNominatim[].class);
            return resultados == null ? List.of() : Arrays.stream(resultados).map(ResultadoNominatim::aDireccion).toList();
        } catch (RestClientException | NumberFormatException e) {
            throw new ServicioExternoNoDisponibleException("El servicio de búsqueda de direcciones no respondió.", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResultadoNominatim(@JsonProperty("display_name") String nombre, String lat, String lon) {

        Direccion aDireccion() {
            return new Direccion(nombre, Double.parseDouble(lat), Double.parseDouble(lon));
        }
    }
}
