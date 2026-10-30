package siante2.capas.adapter.out.wms;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import siante2.capas.domain.Extension;
import siante2.capas.domain.MatrizMosaicos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CapacidadesWmtsTest {

    /** Como el de ArcGIS Online: una grilla en EPSG:4326 (latitud primero) y otra en 3857, plantilla REST. */
    static final String CAPACIDADES = """
            <Capabilities xmlns="http://www.opengis.net/wmts/1.0" xmlns:ows="http://www.opengis.net/ows/1.1"
                xmlns:xlink="http://www.w3.org/1999/xlink" version="1.0.0">
              <ows:OperationsMetadata>
                <ows:Operation name="GetTile"><ows:DCP><ows:HTTP>
                  <ows:Get xlink:href="https://8.8.8.8/wmts/kvp?"/>
                </ows:HTTP></ows:DCP></ows:Operation>
              </ows:OperationsMetadata>
              <Contents>
                <Layer>
                  <ows:Title>Ortofoto 2020</ows:Title>
                  <ows:WGS84BoundingBox><ows:LowerCorner>-76 -56</ows:LowerCorner><ows:UpperCorner>-66 -17</ows:UpperCorner></ows:WGS84BoundingBox>
                  <ows:Identifier>orto</ows:Identifier>
                  <Style><ows:Identifier>otro</ows:Identifier></Style>
                  <Style isDefault="true"><ows:Identifier>normal</ows:Identifier></Style>
                  <Format>image/jpeg</Format>
                  <TileMatrixSetLink><TileMatrixSet>Grados</TileMatrixSet></TileMatrixSetLink>
                  <TileMatrixSetLink><TileMatrixSet>Google</TileMatrixSet></TileMatrixSetLink>
                  <ResourceURL format="image/jpeg" resourceType="tile"
                      template="https://8.8.8.8/rest/orto/{Style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.jpg"/>
                </Layer>
                <Layer>
                  <ows:Identifier>solo-grados</ows:Identifier>
                  <TileMatrixSetLink><TileMatrixSet>Grados</TileMatrixSet></TileMatrixSetLink>
                </Layer>
                <TileMatrixSet>
                  <ows:Identifier>Grados</ows:Identifier>
                  <ows:SupportedCRS>urn:ogc:def:crs:EPSG::4326</ows:SupportedCRS>
                  <TileMatrix><ows:Identifier>0</ows:Identifier><ScaleDenominator>279541132.0143589</ScaleDenominator>
                    <TopLeftCorner>90 -180</TopLeftCorner><TileWidth>256</TileWidth><TileHeight>256</TileHeight>
                    <MatrixWidth>2</MatrixWidth><MatrixHeight>1</MatrixHeight></TileMatrix>
                </TileMatrixSet>
                <TileMatrixSet>
                  <ows:Identifier>Google</ows:Identifier>
                  <ows:SupportedCRS>urn:ogc:def:crs:EPSG::3857</ows:SupportedCRS>
                  <TileMatrix><ows:Identifier>1</ows:Identifier><ScaleDenominator>279541132.0143589</ScaleDenominator>
                    <TopLeftCorner>-20037508.3427892 20037508.3427892</TopLeftCorner><TileWidth>256</TileWidth>
                    <TileHeight>256</TileHeight><MatrixWidth>2</MatrixWidth><MatrixHeight>2</MatrixHeight></TileMatrix>
                  <TileMatrix><ows:Identifier>0</ows:Identifier><ScaleDenominator>559082264.0287178</ScaleDenominator>
                    <TopLeftCorner>-20037508.3427892 20037508.3427892</TopLeftCorner><TileWidth>256</TileWidth>
                    <TileHeight>256</TileHeight><MatrixWidth>1</MatrixWidth><MatrixHeight>1</MatrixHeight></TileMatrix>
                </TileMatrixSet>
              </Contents>
            </Capabilities>""";

    private static CapacidadesWmts.Leidas leer(String xml) {
        return CapacidadesWmts.leer(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void prefiereLaGrillaDelMapaYCalculaLaResolucionDeCadaNivelDeMenosAMasZoom() {
        CapacidadesWmts.Leidas leidas = leer(CAPACIDADES);
        CapacidadesWmts.Grilla grilla = leidas.grillaDelMapa().orElseThrow();

        assertThat(grilla.id()).isEqualTo("Google");
        assertThat(grilla.crs()).isEqualTo("EPSG:3857");
        assertThat(grilla.matrices()).extracting(MatrizMosaicos::id).containsExactly("0", "1");
        MatrizMosaicos cero = grilla.matrices().getFirst();
        assertThat(cero.resolucion()).isCloseTo(156543.0339, within(0.001));
        assertThat(cero.origenX()).isCloseTo(-20037508.34, within(0.01));
        assertThat(cero.origenY()).isCloseTo(20037508.34, within(0.01));
        assertThat(leidas.capasDeServicio(grilla)).singleElement()
                .satisfies(capa -> {
                    assertThat(capa.nombre()).isEqualTo("orto");
                    assertThat(capa.titulo()).isEqualTo("Ortofoto 2020");
                    assertThat(capa.extension()).isEqualTo(new Extension(-76, -56, -66, -17));
                });
        assertThat(leidas.urlGetTile()).isEqualTo("https://8.8.8.8/wmts/kvp?");
    }

    @Test
    void deCadaCapaGuardaElEstiloPorOmisionElFormatoYLaPlantillaRest() {
        CapacidadesWmts.Capa orto = leer(CAPACIDADES).capa("orto").orElseThrow();

        assertThat(orto.estilo()).isEqualTo("normal");
        assertThat(orto.formato()).isEqualTo("image/jpeg");
        assertThat(orto.plantilla()).endsWith("/{TileMatrix}/{TileRow}/{TileCol}.jpg");
        CapacidadesWmts.Capa soloGrados = leer(CAPACIDADES).capa("solo-grados").orElseThrow();
        assertThat(soloGrados.estilo()).isEqualTo("default");
        assertThat(soloGrados.formato()).isEqualTo("image/png");
        assertThat(soloGrados.plantilla()).isEmpty();
    }

    @Test
    void enEpsg4326LaEsquinaVieneConLaLatitudPrimero() {
        String soloGrados = CAPACIDADES.replace("<TileMatrixSet>Google</TileMatrixSet>", "");
        CapacidadesWmts.Grilla grilla = leer(soloGrados).grillaDelMapa().orElseThrow();

        assertThat(grilla.crs()).isEqualTo("EPSG:4326");
        MatrizMosaicos cero = grilla.matrices().getFirst();
        assertThat(cero.origenX()).isEqualTo(-180);
        assertThat(cero.origenY()).isEqualTo(90);
        assertThat(cero.resolucion()).isCloseTo(0.703125, within(0.00001));
    }

    @Test
    void unaGrillaEnOtroSistemaOMalEscritaNoSeUsa() {
        String otroSistema = CAPACIDADES.replace("EPSG::3857", "EPSG::25830").replace("EPSG::4326", "EPSG::32719");
        assertThat(leer(otroSistema).grillaDelMapa()).isEmpty();

        String malEscrita = CAPACIDADES.replace("<MatrixWidth>1</MatrixWidth>", "<MatrixWidth>uno</MatrixWidth>")
                .replace("EPSG::4326", "EPSG::32719");
        assertThat(leer(malEscrita).grillaDelMapa()).isEmpty();
    }

    @Test
    void unXmlQueNoEsWmtsNoSeAcepta() {
        assertThatThrownBy(() -> leer("<WMS_Capabilities/>")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> leer("<html>")).isInstanceOf(IllegalArgumentException.class);
    }
}
