package siante2.capas.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoServicioTest {

    @Test
    void sinNadaQueLoDigaEsWmsComoEnSiate() {
        assertThat(TipoServicio.deUrl("https://ide.cl/geoserver/ows")).isEqualTo(TipoServicio.WMS);
    }

    @Test
    void reconoceElTipoEnElTextoDeLaUrl() {
        assertThat(TipoServicio.deUrl("https://ide.cl/WMS")).isEqualTo(TipoServicio.WMS);
        assertThat(TipoServicio.deUrl("https://ide.cl/gwc/service/WMTS")).isEqualTo(TipoServicio.WMTS);
        assertThat(TipoServicio.deUrl("https://ide.cl/wfs")).isEqualTo(TipoServicio.WFS);
    }

    @Test
    void wmtsGanaAWmsSinImportarElNombreDelServidor() {
        assertThat(TipoServicio.deUrl("https://wms.ide.cl/wmts")).isEqualTo(TipoServicio.WMTS);
    }

    @Test
    void elParametroServiceMandaSobreElRestoDeLaUrl() {
        assertThat(TipoServicio.deUrl("https://wfs.ide.cl/ows?service=WMS&request=GetCapabilities")).isEqualTo(TipoServicio.WMS);
        assertThat(TipoServicio.deUrl("https://ide.cl/wms?request=GetCapabilities&SERVICE=wmts")).isEqualTo(TipoServicio.WMTS);
    }

    @Test
    void unParametroParecidoNoEsElDelServicio() {
        assertThat(TipoServicio.deUrl("https://ide.cl/ows?subservice=WMS")).isEqualTo(TipoServicio.WMS);
        assertThat(TipoServicio.deUrl("https://ide.cl/ows?subservice=WFS")).isEqualTo(TipoServicio.WFS);
    }
}
