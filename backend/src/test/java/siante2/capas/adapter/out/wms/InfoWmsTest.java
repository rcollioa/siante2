package siante2.capas.adapter.out.wms;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.Metadato;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InfoWmsTest {

    private static List<ElementoDeServicio> leer(String cuerpo, String formato) {
        return InfoWms.leer(cuerpo.getBytes(StandardCharsets.UTF_8), formato);
    }

    @Test
    void prefiereGeoJsonLuegoXmlYNuncaHtmlRespetandoComoLoEscribeElServidor() {
        assertThat(InfoWms.formatoPreferido(List.of("text/html", "text/xml", "application/geojson")))
                .contains("application/geojson");
        assertThat(InfoWms.formatoPreferido(List.of("text/plain", "application/vnd.ogc.gml", "TEXT/XML")))
                .contains("TEXT/XML");
        assertThat(InfoWms.formatoPreferido(List.of("text/html", "text/plain"))).isEmpty();
    }

    @Test
    void geoJsonDeGeoServerTomaLaCapaDelIdYDejaFueraLaGeometria() {
        String json = """
                {"type":"FeatureCollection","features":[{"type":"Feature","id":"ide:rios.12",
                 "geometry":{"type":"Point","coordinates":[1,2]},
                 "properties":{"NOMBRE":"Maipo","LARGO":250.5,"VACIO":null,"FORMA":{"a":1}}}]}""";

        assertThat(leer(json, "application/json")).containsExactly(new ElementoDeServicio("ide:rios", List.of(
                new Metadato("NOMBRE", "Maipo"), new Metadato("LARGO", "250.5"), new Metadato("VACIO", ""))));
    }

    @Test
    void geoJsonDeArcGisTomaLaCapaDeLayerName() {
        String json = """
                {"type":"FeatureCollection","features":[{"layerName":"Comunas","properties":{"COMUNA":"Ñuñoa"}}]}""";

        assertThat(leer(json, "application/geojson")).extracting(ElementoDeServicio::capa).containsExactly("Comunas");
    }

    @Test
    void xmlDeArcGis10() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FeatureInfoResponse xmlns:esri_wms="http://www.esri.com/wms" xmlns="http://www.esri.com/wms">
                  <FeatureInfoCollection layername="Comunas">
                    <FeatureInfo><Fields>
                      <Field><FieldName>COMUNA</FieldName><FieldValue>Ñuñoa</FieldValue></Field>
                      <Field><FieldName>REGION</FieldName><FieldValue>13</FieldValue></Field>
                    </Fields></FeatureInfo>
                  </FeatureInfoCollection>
                </FeatureInfoResponse>""";

        assertThat(leer(xml, "text/xml")).containsExactly(new ElementoDeServicio("Comunas",
                List.of(new Metadato("COMUNA", "Ñuñoa"), new Metadato("REGION", "13"))));
    }

    @Test
    void xmlDeArcGisAntiguoConLosDatosComoAtributos() {
        String xml = "<FeatureInfoResponse><FIELDS OBJECTID=\"4\" NOMBRE=\"Maipo\"/></FeatureInfoResponse>";

        assertThat(leer(xml, "application/vnd.ogc.wms_xml")).singleElement()
                .satisfies(e -> assertThat(e.datos()).contains(new Metadato("NOMBRE", "Maipo")));
    }

    @Test
    void gmlDeMapServer() {
        String xml = """
                <msGMLOutput xmlns:gml="http://www.opengis.net/gml">
                  <rios_layer><gml:name>rios</gml:name>
                    <rios_feature>
                      <gml:boundedBy><gml:Box><gml:coordinates>1,2 3,4</gml:coordinates></gml:Box></gml:boundedBy>
                      <NOMBRE>Maipo</NOMBRE>
                    </rios_feature>
                  </rios_layer>
                </msGMLOutput>""";

        assertThat(leer(xml, "application/vnd.ogc.gml"))
                .containsExactly(new ElementoDeServicio("rios", List.of(new Metadato("NOMBRE", "Maipo"))));
    }

    @Test
    void gmlDeGeoServerSinLaGeometria() {
        String xml = """
                <wfs:FeatureCollection xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml"
                    xmlns:ide="http://ide.cl">
                  <gml:featureMember>
                    <ide:rios fid="rios.12">
                      <ide:the_geom><gml:Point><gml:coordinates>1,2</gml:coordinates></gml:Point></ide:the_geom>
                      <ide:NOMBRE>Maipo</ide:NOMBRE>
                    </ide:rios>
                  </gml:featureMember>
                </wfs:FeatureCollection>""";

        assertThat(leer(xml, "application/vnd.ogc.gml"))
                .containsExactly(new ElementoDeServicio("rios", List.of(new Metadato("NOMBRE", "Maipo"))));
    }

    @Test
    void unErrorDelServidorOUnaRespuestaQueNoEsDelFormatoNoSeAcepta() {
        assertThatThrownBy(() -> leer("<ServiceExceptionReport><ServiceException>Capa no consultable</ServiceException>"
                + "</ServiceExceptionReport>", "text/xml"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Capa no consultable");
        assertThatThrownBy(() -> leer("<html><body>Error", "text/xml")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> leer("<html>", "application/json")).isInstanceOf(IllegalArgumentException.class);
        assertThat(leer("{}", "application/json")).isEmpty();
    }
}
