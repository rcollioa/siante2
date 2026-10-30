package siante2.capas.adapter.out.wms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.PedidoMosaico;
import siante2.capas.domain.ServicioMapa;
import siante2.capas.domain.TipoServicio;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Con un servidor WMTS simulado. 8.8.8.8 en vez de un nombre: la prueba no depende de resolver DNS. */
class ClienteWmtsTest {

    private static final String URL = "https://8.8.8.8/WMTSCapabilities.xml";

    private MockRestServiceServer servidor;
    private ClienteWmts cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = new ClienteWmts(new ServidoresMapa(constructor.build()), new ConcurrentMapCacheManager("capacidades-wmts"));
    }

    private void capacidades(String xml) {
        servidor.expect(requestTo(URL)).andRespond(withSuccess(xml, MediaType.APPLICATION_XML));
    }

    @Test
    void leeElServicioUnaVezYPideCadaMosaicoConLaPlantillaDeLaCapa() {
        capacidades(CapacidadesWmtsTest.CAPACIDADES);
        servidor.expect(requestTo("https://8.8.8.8/rest/orto/normal/Google/1/0/1.jpg"))
                .andRespond(withSuccess(new byte[] {7}, MediaType.IMAGE_JPEG));

        ServicioMapa servicio = cliente.servicioWmts(URL);
        assertThat(servicio.tipo()).isEqualTo(TipoServicio.WMTS);
        assertThat(servicio.capas()).extracting(CapaDeServicio::nombre).containsExactly("orto");
        assertThat(servicio.grilla().crs()).isEqualTo("EPSG:3857");
        assertThat(cliente.mosaicoWmts(URL, new PedidoMosaico("orto", "1", 0, 1)).contenido()).containsExactly(7);
        servidor.verify();
    }

    @Test
    void sinUnaGrillaQueElMapaSepaDibujarElServicioNoEstaDisponible() {
        capacidades(CapacidadesWmtsTest.CAPACIDADES.replace("EPSG::3857", "EPSG::25830").replace("EPSG::4326", "EPSG::32719"));

        assertThatThrownBy(() -> cliente.servicioWmts(URL))
                .isInstanceOf(ServicioExternoNoDisponibleException.class)
                .hasMessageContaining("sistema de coordenadas");
    }

    @Test
    void unServidorQueNoRespondeEsServicioNoDisponibleYNoQuedaEnCache() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        capacidades(CapacidadesWmtsTest.CAPACIDADES);

        assertThatThrownBy(() -> cliente.servicioWmts(URL)).isInstanceOf(ServicioExternoNoDisponibleException.class);
        assertThat(cliente.servicioWmts(URL).capas()).hasSize(1);
        servidor.verify();
    }

    @Test
    void unaCapaQueElServidorYaNoPublicaNoSePide() {
        capacidades(CapacidadesWmtsTest.CAPACIDADES);

        assertThatThrownBy(() -> cliente.mosaicoWmts(URL, new PedidoMosaico("borrada", "0", 0, 0)))
                .isInstanceOf(ServicioExternoNoDisponibleException.class);
        servidor.verify();
    }
}
