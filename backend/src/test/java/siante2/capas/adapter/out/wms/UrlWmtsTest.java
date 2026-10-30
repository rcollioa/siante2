package siante2.capas.adapter.out.wms;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import siante2.capas.domain.PedidoMosaico;

import static org.assertj.core.api.Assertions.assertThat;

class UrlWmtsTest {

    private static final CapacidadesWmts.Leidas LEIDAS =
            CapacidadesWmts.leer(CapacidadesWmtsTest.CAPACIDADES.getBytes(StandardCharsets.UTF_8));

    @Test
    void elGetCapabilitiesSeUsaTalCualSiEsUnXmlOYaLoPideYSiNoSeLeAgregaKvp() {
        assertThat(UrlWmts.capacidades("https://ide.cl/wmts/1.0.0/WMTSCapabilities.xml?token=1").toString())
                .isEqualTo("https://ide.cl/wmts/1.0.0/WMTSCapabilities.xml?token=1");
        assertThat(UrlWmts.capacidades("https://ide.cl/wmts?request=GetCapabilities&service=WMTS").toString())
                .isEqualTo("https://ide.cl/wmts?request=GetCapabilities&service=WMTS");
        assertThat(UrlWmts.capacidades("https://ide.cl/geoserver/gwc/service/wmts?map=chile").toString())
                .isEqualTo("https://ide.cl/geoserver/gwc/service/wmts?map=chile&SERVICE=WMTS&REQUEST=GetCapabilities");
    }

    @Test
    void conPlantillaRestReemplazaSusVariables() {
        CapacidadesWmts.Capa orto = LEIDAS.capa("orto").orElseThrow();

        assertThat(UrlWmts.mosaico("https://8.8.8.8/WMTSCapabilities.xml", LEIDAS, orto, "Google",
                new PedidoMosaico("orto", "5", 10, 20)).toString())
                .isEqualTo("https://8.8.8.8/rest/orto/normal/Google/5/10/20.jpg");
    }

    @Test
    void sinPlantillaPideKvpEnLaUrlDeGetTileDelServidor() {
        CapacidadesWmts.Capa soloGrados = LEIDAS.capa("solo-grados").orElseThrow();

        assertThat(UrlWmts.mosaico("https://8.8.8.8/WMTSCapabilities.xml", LEIDAS, soloGrados, "Grados",
                new PedidoMosaico("solo-grados", "EPSG:4326:0", 0, 1)).toString())
                .isEqualTo("https://8.8.8.8/wmts/kvp?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=solo-grados"
                        + "&STYLE=default&FORMAT=image%2Fpng&TILEMATRIXSET=Grados&TILEMATRIX=EPSG%3A4326%3A0&TILEROW=0&TILECOL=1");
    }

    @Test
    void sinUrlDeGetTileUsaLaDelServicioConservandoSusParametrosPropios() {
        CapacidadesWmts.Leidas sinGetTile = new CapacidadesWmts.Leidas(LEIDAS.capas(), LEIDAS.grillas(), "");
        CapacidadesWmts.Capa soloGrados = LEIDAS.capa("solo-grados").orElseThrow();

        assertThat(UrlWmts.mosaico("https://ide.cl/wmts?map=chile&request=GetCapabilities", sinGetTile, soloGrados,
                "Grados", new PedidoMosaico("solo-grados", "0", 0, 0)).toString())
                .startsWith("https://ide.cl/wmts?map=chile&SERVICE=WMTS&REQUEST=GetTile");
    }
}
