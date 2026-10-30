package siante2.capas.adapter.out.wms;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.PedidoMapa;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlWmsTest {

    private static PedidoMapa pedido(String version) {
        return new PedidoMapa(List.of("ide:rios", "ide:lagos"), "-7.9e6,-4e6,-7.8e6,-3.9e6", 256, 256, "EPSG:3857",
                "image/png", true, version);
    }

    @Test
    void getCapabilitiesConservaLosParametrosPropiosDelServidorYReemplazaLosDeWms() {
        URI uri = UrlWms.capacidades("https://mapas.cl/cgi-bin/mapserv?map=/srv/chile.map&request=GetCapabilities&service=WMS");

        assertThat(uri.toString())
                .isEqualTo("https://mapas.cl/cgi-bin/mapserv?map=/srv/chile.map&SERVICE=WMS&REQUEST=GetCapabilities");
    }

    @Test
    void getMap130UsaCrsY111UsaSrsConLosValoresCodificados() {
        String url = "https://ide.cl/geoserver/wms?service=WMS&version=1.1.1&request=GetCapabilities";

        assertThat(UrlWms.mapa(url, pedido("1.3.0")).toString()).isEqualTo("https://ide.cl/geoserver/wms?SERVICE=WMS"
                + "&REQUEST=GetMap&VERSION=1.3.0&LAYERS=ide%3Arios%2Cide%3Alagos&STYLES=&CRS=EPSG%3A3857"
                + "&BBOX=-7.9e6%2C-4e6%2C-7.8e6%2C-3.9e6&WIDTH=256&HEIGHT=256&FORMAT=image%2Fpng&TRANSPARENT=TRUE");
        assertThat(UrlWms.mapa(url, pedido("1.1.1")).toString()).contains("&SRS=EPSG%3A3857").doesNotContain("CRS=");
    }

    @Test
    void getFeatureInfoPideLaMismaImagenYElPixelSegunLaVersion() {
        String url = "https://ide.cl/geoserver/wms?map=chile&request=GetCapabilities&i=99";

        assertThat(UrlWms.info(url, new ConsultaPunto(pedido("1.3.0"), 10, 20), "application/json").toString())
                .startsWith("https://ide.cl/geoserver/wms?map=chile&SERVICE=WMS&REQUEST=GetFeatureInfo&VERSION=1.3.0")
                .contains("&QUERY_LAYERS=ide%3Arios%2Cide%3Alagos&INFO_FORMAT=application%2Fjson&I=10&J=20&FEATURE_COUNT=10")
                .doesNotContain("i=99");
        assertThat(UrlWms.info(url, new ConsultaPunto(pedido("1.1.1"), 10, 20), "text/xml").toString())
                .contains("&SRS=EPSG%3A3857").contains("&X=10&Y=20").doesNotContain("&I=");
    }

    @Test
    void unaUrlGetMapTraeSusCapasSinPreguntarAlServidor() {
        assertThat(UrlWms.desdeGetMap("https://ide.cl/wms?REQUEST=GetMap&LAYERS=rios,lagos&BBOX=1,2,3,4"))
                .hasValueSatisfying(capas -> assertThat(capas).extracting(CapaDeServicio::nombre).containsExactly("rios", "lagos"));
        assertThat(UrlWms.desdeGetMap("https://ide.cl/wms?request=GetCapabilities")).isEmpty();
    }

    @Test
    void soloHttpYNuncaLaPropiaMaquinaNiMetadatosDeLaNube() {
        UrlWms.permitida(URI.create("https://8.8.8.8/wms"));

        for (String prohibida : List.of("http://localhost:8081/actuator", "http://127.0.0.1/", "http://169.254.169.254/latest",
                "http://0.0.0.0/", "ftp://ide.cl/wms", "file:///etc/passwd")) {
            assertThatThrownBy(() -> UrlWms.permitida(URI.create(prohibida)))
                    .as(prohibida).isInstanceOf(ServicioExternoNoDisponibleException.class);
        }
    }
}
