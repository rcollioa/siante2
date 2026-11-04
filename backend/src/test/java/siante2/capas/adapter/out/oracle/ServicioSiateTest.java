package siante2.capas.adapter.out.oracle;

import org.junit.jupiter.api.Test;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.TipoServicio;

import static org.assertj.core.api.Assertions.assertThat;

class ServicioSiateTest {

    @Test
    void leeElTipoYLaUrlComoLosGuardaSiate() {
        assertThat(ServicioSiate.leer("{\"type\":\"WMS\",\"url\":\"https://ide.cl/wms?request=GetCapabilities\"}"))
                .contains(new OrigenServicio(TipoServicio.WMS, "https://ide.cl/wms?request=GetCapabilities"));
        assertThat(ServicioSiate.leer("[{\"type\":\"wmts\",\"url\":\" https://ide.cl/wmts \"}]"))
                .contains(new OrigenServicio(TipoServicio.WMTS, "https://ide.cl/wmts"));
    }

    @Test
    void escribeComoSiateConElTipoPrimeroYSeVuelveALeer() {
        OrigenServicio servicio = new OrigenServicio(TipoServicio.WMTS, "https://ide.cl/wmts?a=\"b\"&c=d");

        String json = ServicioSiate.escribir(servicio);

        assertThat(json).startsWith("{\"type\":\"WMTS\",\"url\":");
        assertThat(ServicioSiate.leer(json)).contains(servicio);
    }

    @Test
    void sinServidorConocidoOUrlHttpNoHayServicio() {
        assertThat(ServicioSiate.leer("{\"type\":\"FeatureCollection\",\"features\":[]}")).isEmpty();
        assertThat(ServicioSiate.leer("{\"type\":\"WMS\",\"url\":\"file:///etc/passwd\"}")).isEmpty();
        assertThat(ServicioSiate.leer("{\"type\":\"WMS\"}")).isEmpty();
        assertThat(ServicioSiate.leer("no es json")).isEmpty();
        assertThat(ServicioSiate.leer("")).isEmpty();
        assertThat(ServicioSiate.leer(null)).isEmpty();
    }
}
