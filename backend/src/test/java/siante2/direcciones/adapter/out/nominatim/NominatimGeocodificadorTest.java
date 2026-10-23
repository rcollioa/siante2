package siante2.direcciones.adapter.out.nominatim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import siante2.compartido.adapter.out.CacheConfig;
import siante2.compartido.adapter.out.ResilienciaConfig;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.direcciones.application.port.out.GeocodificadorPort;
import siante2.direcciones.domain.Direccion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Con caché y reintento reales (proxies de clase, como en Spring Boot) y Nominatim simulado. */
@SpringJUnitConfig(NominatimGeocodificadorTest.Configuracion.class)
@TestPropertySource(properties = "app.geocodificacion.paises=cl")
class NominatimGeocodificadorTest {

    private static final String RESPUESTA = """
            [{"place_id": 1, "display_name": "Teatinos 56, Santiago, Chile", "lat": "-33.4405", "lon": "-70.6539"}]
            """;

    @Autowired
    private GeocodificadorPort geocodificador;

    @Autowired
    private MockRestServiceServer nominatim;

    @BeforeEach
    void reiniciar() {
        nominatim.reset();
    }

    @Test
    void consultaNominatimYLaSegundaVezSaleDeLaCache() {
        nominatim.expect(once(), requestTo(startsWith("https://nominatim.prueba/search")))
                .andExpect(queryParam("q", "teatinos%2056"))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(queryParam("countrycodes", "cl"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        Direccion esperada = new Direccion("Teatinos 56, Santiago, Chile", -33.4405, -70.6539);
        assertThat(geocodificador.buscar("teatinos 56")).containsExactly(esperada);
        assertThat(geocodificador.buscar("teatinos 56")).containsExactly(esperada);
        nominatim.verify();
    }

    @Test
    void reintentaUnaVezSiNominatimFalla() {
        nominatim.expect(requestTo(startsWith("https://nominatim.prueba/search")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        nominatim.expect(requestTo(startsWith("https://nominatim.prueba/search")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(geocodificador.buscar("valparaiso")).isEmpty();
        nominatim.verify();
    }

    @Test
    void siFallaDosVecesAvisaQueNoEstaDisponibleYNoGuardaElError() {
        nominatim.expect(twice(), requestTo(startsWith("https://nominatim.prueba/search")))
                .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));

        assertThatThrownBy(() -> geocodificador.buscar("temuco"))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
        nominatim.verify();

        nominatim.reset();
        nominatim.expect(once(), requestTo(startsWith("https://nominatim.prueba/search")))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));
        assertThat(geocodificador.buscar("temuco")).hasSize(1);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(AopAutoConfiguration.class)
    @Import({CacheConfig.class, ResilienciaConfig.class, NominatimGeocodificador.class})
    static class Configuracion {

        private final RestClient.Builder builder = RestClient.builder().baseUrl("https://nominatim.prueba");

        @Bean
        MockRestServiceServer nominatim() {
            return MockRestServiceServer.bindTo(builder).build();
        }

        @Bean
        RestClient nominatimRestClient(MockRestServiceServer nominatim) {
            return builder.build();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("direcciones");
        }
    }
}
