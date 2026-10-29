package siante2.capas.adapter.out.wms;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.Extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class CapacidadesWmsTest {

    @Test
    void wms130LeeLasCapasFinalesConTituloYExtensionHeredadaDelGrupo() {
        String xml = """
                <WMS_Capabilities version="1.3.0" xmlns="http://www.opengis.net/wms">
                  <Capability>
                    <Layer>
                      <Title>Servidor IDE</Title>
                      <EX_GeographicBoundingBox>
                        <westBoundLongitude>-76</westBoundLongitude><eastBoundLongitude>-66</eastBoundLongitude>
                        <southBoundLatitude>-56</southBoundLatitude><northBoundLatitude>-17</northBoundLatitude>
                      </EX_GeographicBoundingBox>
                      <Layer><Name>ide:rios</Name><Title><![CDATA[Ríos]]></Title></Layer>
                      <Layer>
                        <Name>ide:lagos</Name><Title>Lagos</Title>
                        <EX_GeographicBoundingBox>
                          <westBoundLongitude>-73</westBoundLongitude><eastBoundLongitude>-71</eastBoundLongitude>
                          <southBoundLatitude>-42</southBoundLatitude><northBoundLatitude>-39</northBoundLatitude>
                        </EX_GeographicBoundingBox>
                      </Layer>
                    </Layer>
                  </Capability>
                </WMS_Capabilities>""";

        assertThat(CapacidadesWms.leer(xml.getBytes(StandardCharsets.UTF_8)))
                .extracting(CapaDeServicio::nombre, CapaDeServicio::titulo, CapaDeServicio::extension)
                .containsExactly(
                        tuple("ide:rios", "Ríos", new Extension(-76, -56, -66, -17)),
                        tuple("ide:lagos", "Lagos", new Extension(-73, -42, -71, -39)));
    }

    @Test
    void wms111UsaLatLonBoundingBoxYSinTituloUsaElNombre() {
        String xml = """
                <WMT_MS_Capabilities version="1.1.1"><Capability><Layer>
                  <Layer><Name>comunas</Name><LatLonBoundingBox minx="-75" miny="-55" maxx="-67" maxy="-18"/></Layer>
                </Layer></Capability></WMT_MS_Capabilities>""";

        assertThat(CapacidadesWms.leer(xml.getBytes(StandardCharsets.UTF_8))).containsExactly(
                new CapaDeServicio("comunas", "comunas", new Extension(-75, -55, -67, -18)));
    }

    @Test
    void siSoloHayGruposConNombreUsaEsos() {
        String xml = """
                <WMS_Capabilities><Capability>
                  <Layer><Name>todo</Name><Title>Todo</Title><Layer><Title>Sin nombre</Title></Layer></Layer>
                </Capability></WMS_Capabilities>""";

        assertThat(CapacidadesWms.leer(xml.getBytes(StandardCharsets.UTF_8))).extracting(CapaDeServicio::nombre).containsExactly("todo");
    }

    @Test
    void rechazaXmlConDoctypeQuePodriaLeerArchivosDelServidor() {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE x [<!ENTITY secreto SYSTEM "file:///etc/passwd">]>
                <WMS_Capabilities><Capability><Layer><Name>&secreto;</Name></Layer></Capability></WMS_Capabilities>""";

        assertThatThrownBy(() -> CapacidadesWms.leer(xml.getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unaRespuestaQueNoEsXmlNoSeAcepta() {
        assertThatThrownBy(() -> CapacidadesWms.leer("<html>Error 500".getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CapacidadesWms.leer(new byte[0])).isInstanceOf(IllegalArgumentException.class);
    }
}
