package siante2.capas.adapter.out.wms;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.PedidoMapa;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Con un servidor WMS simulado. 8.8.8.8 en vez de un nombre: la prueba no depende de resolver DNS. */
class ClienteWmsTest {

    private static final String URL = "https://8.8.8.8/geoserver/wms?request=GetCapabilities";
    private static final PedidoMapa PEDIDO = new PedidoMapa(List.of("rios"), "0,0,10,10", 256, 256, "EPSG:3857",
            "image/png", true, "1.3.0");

    private MockRestServiceServer servidor;
    private ClienteWms cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(constructor).build();
        cliente = new ClienteWms(constructor.build());
    }

    @Test
    void leeLasCapasDelGetCapabilities() {
        servidor.expect(requestTo("https://8.8.8.8/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"))
                .andRespond(withSuccess(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><WMS_Capabilities><Capability><Layer>"
                        + "<Name>rios</Name><Title>Ríos</Title></Layer></Capability></WMS_Capabilities>")
                        .getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_XML));

        assertThat(cliente.capasWms(URL)).extracting(CapaDeServicio::titulo).containsExactly("Ríos");
    }

    @Test
    void unServidorQueNoRespondeOEntregaOtraCosaEsServicioNoDisponible() {
        servidor.expect(requestTo("https://8.8.8.8/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"))
                .andRespond(withServerError());
        assertThatThrownBy(() -> cliente.capasWms(URL)).isInstanceOf(ServicioExternoNoDisponibleException.class);

        servidor.reset();
        servidor.expect(requestTo("https://8.8.8.8/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"))
                .andRespond(withSuccess("<WMS_Capabilities><Capability/></WMS_Capabilities>", MediaType.APPLICATION_XML));
        assertThatThrownBy(() -> cliente.capasWms(URL)).hasMessage("El servidor de mapas de la capa no publica capas.");
    }

    @Test
    void entregaLaImagenConSuTipo() {
        servidor.expect(requestTo(startsWith("https://8.8.8.8/geoserver/wms?SERVICE=WMS&REQUEST=GetMap")))
                .andRespond(withSuccess(new byte[] {1, 2, 3}, MediaType.IMAGE_PNG));

        assertThat(cliente.mapaWms(URL, PEDIDO).tipo()).isEqualTo("image/png");
    }

    @Test
    void unErrorWmsEnXmlNoSeEntregaComoImagen() {
        servidor.expect(requestTo(startsWith("https://8.8.8.8/geoserver/wms?")))
                .andRespond(withSuccess("<ServiceExceptionReport/>", MediaType.parseMediaType("application/vnd.ogc.se_xml")));

        assertThatThrownBy(() -> cliente.mapaWms(URL, PEDIDO))
                .isInstanceOf(ServicioExternoNoDisponibleException.class)
                .hasMessage("El servidor de mapas de la capa no entregó una imagen.");
    }
}
