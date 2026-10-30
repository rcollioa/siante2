package siante2.capas.adapter.out.wms;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.PedidoMapa;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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
        cliente = new ClienteWms(new ServidoresMapa(constructor.build()));
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

    private void capacidadesConInfo(String... formatos) {
        StringBuilder xml = new StringBuilder("<WMS_Capabilities><Capability><Request><GetFeatureInfo>");
        for (String formato : formatos) {
            xml.append("<Format>").append(formato).append("</Format>");
        }
        xml.append("</GetFeatureInfo></Request><Layer><Name>rios</Name></Layer></Capability></WMS_Capabilities>");
        servidor.expect(requestTo("https://8.8.8.8/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"))
                .andRespond(withSuccess(xml.toString(), MediaType.APPLICATION_XML));
    }

    private static ConsultaPunto consulta() {
        return new ConsultaPunto(PEDIDO, 10, 20);
    }

    @Test
    void consultaElPuntoEnElFormatoQueElServidorOfreceYRecuerdaElFormato() {
        capacidadesConInfo("text/html", "application/json");
        servidor.expect(requestTo(containsString("REQUEST=GetFeatureInfo")))
                .andExpect(requestTo(containsString("INFO_FORMAT=application%2Fjson")))
                .andRespond(withSuccess("{\"features\":[{\"id\":\"rios.1\",\"properties\":{\"NOMBRE\":\"Maipo\"}}]}",
                        MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(containsString("REQUEST=GetFeatureInfo")))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertThat(cliente.infoWms(URL, consulta())).extracting(ElementoDeServicio::capa).containsExactly("rios");
        assertThat(cliente.infoWms(URL, consulta())).isEmpty();
        servidor.verify();
    }

    @Test
    void sinUnFormatoQueSeSepaLeerNoSeConsultaAlServidor() {
        capacidadesConInfo("text/html", "text/plain");

        assertThat(cliente.infoWms(URL, consulta())).isEmpty();
        servidor.verify();
    }

    @Test
    void unaRespuestaIlegibleNoMuestraNadaYUnServidorCaidoEsServicioNoDisponible() {
        capacidadesConInfo("text/xml");
        servidor.expect(requestTo(containsString("REQUEST=GetFeatureInfo")))
                .andRespond(withSuccess("<html>Error", MediaType.TEXT_HTML));
        servidor.expect(requestTo(containsString("REQUEST=GetFeatureInfo"))).andRespond(withServerError());

        assertThat(cliente.infoWms(URL, consulta())).isEmpty();
        assertThatThrownBy(() -> cliente.infoWms(URL, consulta())).isInstanceOf(ServicioExternoNoDisponibleException.class);
    }

    @Test
    void unServicioDadoDeBajaSeInformaComoTal() {
        servidor.expect(requestTo("https://8.8.8.8/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("<!DOCTYPE html><title>Not Found</title>"));

        assertThatThrownBy(() -> cliente.capasWms(URL))
                .isInstanceOf(ServicioExternoNoDisponibleException.class)
                .hasMessage("El servidor de mapas ya no publica el servicio de la capa (respondió 404).");
    }

    @Test
    void sigueLasRedireccionesComoGeoportalYLuegoPideDirectoAlDestinoFinal() {
        String capacidades = "/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities";
        servidor.expect(requestTo("http://8.8.8.8" + capacidades))
                .andRespond(withStatus(HttpStatus.PERMANENT_REDIRECT).location(URI.create("https://8.8.8.8" + capacidades)));
        servidor.expect(requestTo("https://8.8.8.8" + capacidades))
                .andRespond(withStatus(HttpStatus.MOVED_PERMANENTLY).location(URI.create("https://8.8.4.4" + capacidades)));
        servidor.expect(requestTo("https://8.8.4.4" + capacidades))
                .andRespond(withSuccess("<WMS_Capabilities><Capability><Layer><Name>rios</Name></Layer></Capability></WMS_Capabilities>"
                        .getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_XML));
        // El mosaico va directo al último servidor: sin repetir las dos redirecciones por cada imagen.
        servidor.expect(requestTo(startsWith("https://8.8.4.4/geoserver/wms?SERVICE=WMS&REQUEST=GetMap")))
                .andRespond(withSuccess(new byte[] {1}, MediaType.IMAGE_PNG));

        String url = "http://8.8.8.8/geoserver/wms?request=GetCapabilities";
        assertThat(cliente.capasWms(url)).extracting(CapaDeServicio::nombre).containsExactly("rios");
        assertThat(cliente.mapaWms(url, PEDIDO).tipo()).isEqualTo("image/png");
        servidor.verify();
    }

    @Test
    void unaRedireccionHaciaLaPropiaMaquinaSeBloquea() {
        servidor.expect(requestTo("https://8.8.8.8/geoserver/wms?SERVICE=WMS&REQUEST=GetCapabilities"))
                .andRespond(withStatus(HttpStatus.FOUND).location(URI.create("http://127.0.0.1:8081/actuator/env")));

        assertThatThrownBy(() -> cliente.capasWms(URL))
                .isInstanceOf(ServicioExternoNoDisponibleException.class)
                .hasMessage("La dirección del servidor de mapas no está permitida.");
    }

    @Test
    void demasiadasRedireccionesNoSeSiguen() {
        servidor.expect(manyTimes(),
                        requestTo(startsWith("https://8.8.8.8/")))
                .andRespond(withStatus(HttpStatus.FOUND).location(URI.create("https://8.8.8.8/otra")));

        assertThatThrownBy(() -> cliente.capasWms(URL)).hasMessage("El servidor de mapas de la capa redirige demasiadas veces.");
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
