package siante2.direcciones.adapter.out.photon;

import java.util.Optional;
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
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.direcciones.application.port.out.AutocompletadoPort;
import siante2.direcciones.domain.Direccion;
import siante2.direcciones.domain.ZonaVisible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringJUnitConfig(PhotonAutocompletadoTest.Configuracion.class)
@TestPropertySource(properties = {
        "app.autocompletado.recuadro=-109.5,-56.0,-66.0,-17.4",
        "app.autocompletado.paises=cl"
})
class PhotonAutocompletadoTest {

    private static final String SANTIAGO = "Región Metropolitana de Santiago";

    /** Una dirección (calle y número), un lugar con nombre y uno sin coordenadas, que se descarta. */
    private static final String RESPUESTA = """
            {"type": "FeatureCollection", "features": [
              {"geometry": {"type": "Point", "coordinates": [-70.6539, -33.4405]},
               "properties": {"street": "Teatinos", "housenumber": "56", "city": "Santiago",
                              "state": "Región Metropolitana de Santiago", "countrycode": "CL"}},
              {"geometry": {"type": "Point", "coordinates": [-70.6536, -33.4429]},
               "properties": {"name": "Palacio de La Moneda", "street": "Moneda", "city": "Santiago",
                              "state": "Región Metropolitana de Santiago", "countrycode": "CL"}},
              {"properties": {"name": "Sin geometría", "countrycode": "CL"}}
            ]}
            """;

    @Autowired
    private AutocompletadoPort autocompletado;

    @Autowired
    private MockRestServiceServer photon;

    @Autowired
    private CacheManager caches;

    @BeforeEach
    void reiniciar() {
        photon.reset();
        caches.getCache("sugerencias").clear();
    }

    @Test
    void sugiereDentroDeChileConNombresLegiblesYLaSegundaVezSaleDeLaCache() {
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andExpect(queryParam("q", "teatinos"))
                .andExpect(queryParam("limit", "10"))
                .andExpect(queryParam("bbox", "-109.5,-56.0,-66.0,-17.4"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        assertThat(autocompletado.sugerir("teatinos", Optional.empty())).containsExactly(
                new Direccion("Teatinos 56, Santiago, " + SANTIAGO, -33.4405, -70.6539),
                new Direccion("Palacio de La Moneda, Santiago, " + SANTIAGO, -33.4429, -70.6536));
        assertThat(autocompletado.sugerir("teatinos", Optional.empty())).hasSize(2);
        photon.verify();
    }

    @Test
    void conLaZonaDelMapaPideOrdenarPorCercaniaYCadaZonaTieneSuCache() {
        ZonaVisible laSerena = new ZonaVisible(-29.9, -71.25, 10);
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andExpect(queryParam("lat", "-29.9"))
                .andExpect(queryParam("lon", "-71.25"))
                .andExpect(queryParam("zoom", "10"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andExpect(queryParam("lat", "-33.44"))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        autocompletado.sugerir("plaza de armas", Optional.of(laSerena));
        autocompletado.sugerir("plaza de armas", Optional.of(laSerena));
        autocompletado.sugerir("plaza de armas", Optional.of(new ZonaVisible(-33.44, -70.65, 10)));
        photon.verify();
    }

    @Test
    void conElMapaMuyCercaPideLaCercaniaDeUnaCiudad() {
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andExpect(queryParam("zoom", String.valueOf(PhotonAutocompletado.ZOOM_MAXIMO_REFERENCIA)))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        autocompletado.sugerir("san martin", Optional.of(new ZonaVisible(-35.41, -71.13, 17)));
        photon.verify();
    }

    @Test
    void descartaLosDeOtrosPaisesLosRepetidosYMuestraCincoComoMaximo() {
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andRespond(withSuccess("""
                        {"features": [
                          %s, %s, %s, %s, %s, %s, %s, %s
                        ]}
                        """.formatted(
                        lugar("San Martín", "Mendoza", "AR"),
                        lugar("Teatinos", "Santiago", "CL"),
                        lugar("TEATINOS", "Santiágo", "cl"),
                        lugar("Teatinos", "La Serena", "CL"),
                        lugar("Teatinos", "San Clemente", null),
                        lugar("Teatinos", "Antofagasta", "CL"),
                        lugar("Teatinos", "Talca", "CL"),
                        lugar("Teatinos", "Temuco", "CL")), MediaType.APPLICATION_JSON));

        assertThat(autocompletado.sugerir("teatinos", Optional.empty()))
                .extracting(Direccion::nombre)
                .containsExactly("Teatinos, Santiago", "Teatinos, La Serena", "Teatinos, Antofagasta",
                        "Teatinos, Talca", "Teatinos, Temuco");
    }

    @Test
    void siElNombreYaEstaEnLaCalleNoLoRepite() {
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andRespond(withSuccess("""
                        {"features": [
                          {"geometry": {"coordinates": [-70.6513, -33.4374]},
                           "properties": {"name": "Plaza de Armas", "street": "Plaza de Armas de Santiago de Chile",
                                          "city": "Santiago", "state": "Región Metropolitana de Santiago",
                                          "countrycode": "CL"}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(autocompletado.sugerir("plaza de armas", Optional.empty()))
                .extracting(Direccion::nombre)
                .containsExactly("Plaza de Armas de Santiago de Chile, Santiago, " + SANTIAGO);
    }

    @Test
    void siPhotonFallaAvisaSinReintentarYNoGuardaElError() {
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> autocompletado.sugerir("temuco", Optional.empty()))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
        photon.verify();

        photon.reset();
        photon.expect(once(), requestTo(startsWith("https://photon.prueba/api")))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));
        assertThat(autocompletado.sugerir("temuco", Optional.empty())).hasSize(2);
    }

    private static String lugar(String nombre, String ciudad, String pais) {
        String codigo = pais == null ? "" : ", \"countrycode\": \"%s\"".formatted(pais);
        return """
                {"geometry": {"coordinates": [-70.65, -33.44]},
                 "properties": {"name": "%s", "city": "%s"%s}}""".formatted(nombre, ciudad, codigo);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(AopAutoConfiguration.class)
    @Import({CacheConfig.class, PhotonAutocompletado.class})
    static class Configuracion {

        private final RestClient.Builder builder = RestClient.builder().baseUrl("https://photon.prueba");

        @Bean
        MockRestServiceServer photon() {
            return MockRestServiceServer.bindTo(builder).build();
        }

        @Bean
        RestClient photonRestClient(MockRestServiceServer photon) {
            return builder.build();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("sugerencias");
        }
    }
}
