package siante2.capas.adapter.out.oracle;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import siante2.capas.domain.CambiosElementos;
import siante2.capas.domain.CapaImportada;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.OrigenArchivo;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.SolicitudInvalidaException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** El SQL real sobre H2: SDO_UTIL.FROM_WKTGEOMETRY es aquí una función que devuelve el WKT, y SPATIAL_OBJECT, un texto.
 * Oracle Spatial se cubre en {@link OracleElementosRepositoryOracleTest}. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(OracleElementosRepository.class)
public class OracleElementosRepositoryTest {

    private static final int YO = 7;
    private static final int OTRO = 8;
    private static final int CAPA = 50;
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final EstiloElemento LINEA = new EstiloElemento("#ff0000", 3, "rgba(1,2,3,0.5)", "");

    @Autowired
    private OracleElementosRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    /** Lo llama H2 en lugar de Oracle Spatial; por eso la clase y el método son públicos. */
    public static String desdeWkt(Clob wkt, Integer srid) throws SQLException, IOException {
        try (Reader lector = wkt.getCharacterStream()) {
            return srid + ":" + lector.readAllAsString();
        }
    }

    @BeforeEach
    void preparar() {
        jdbc.execute("alter table LAYERS_FEATURES alter column JSON_METADATA clob");
        jdbc.execute("alter table LAYERS alter column JSON_METADATA clob");
        jdbc.execute("alter table LAYERS_FEATURES add column if not exists SPATIAL_OBJECT clob");
        jdbc.execute("create schema if not exists SDO_UTIL");
        jdbc.execute("create alias if not exists SDO_UTIL.FROM_WKTGEOMETRY for \"" + getClass().getName() + ".desdeWkt\"");
        jdbc.update("delete from LAYERS_FEATURES");
        jdbc.update("delete from LAYERS");
        capa(CAPA, YO, "0", "1", null);
    }

    private void capa(int id, int dueno, String visibilidad, String estado, String json) {
        jdbc.update("insert into LAYERS (ID, OWNER, NAME, STATUS, JSON_METADATA, VISIBILITY) values (?, ?, 'Capa', ?, ?, ?)",
                id, dueno, estado, json, visibilidad);
    }

    private void fila(int capa, int id, String json) {
        jdbc.update("insert into LAYERS_FEATURES (ID_FEATURE, ID_LAYER, TYPE, DATE_CREATE, JSON_METADATA) "
                + "values (?, ?, 'Point', timestamp '2020-01-02 03:04:05', ?)", id, capa, json);
    }

    private static ElementoCapa punto(int id, double longitud, double latitud) {
        return new ElementoCapa(id, Geometria.simple("Point", List.of(longitud, latitud)), 0, "Punto", "", EstiloElemento.SIN_ESTILO,
                List.of());
    }

    private static ElementoCapa linea(int id) {
        return new ElementoCapa(id, Geometria.simple("LineString", List.of(List.of(-70.6, -33.4), List.of(-70.5, -33.3))), 0,
                "Ruta", "A la bodega", LINEA, List.of(new Metadato("Fuente", "Terreno"), new Metadato(" ", "sin nombre")));
    }

    private static CambiosElementos nuevos(ElementoCapa... elementos) {
        return new CambiosElementos(List.of(elementos), List.of(), List.of());
    }

    private Map<String, Object> filaGuardada(int capa, int id) {
        return jdbc.queryForMap("select * from LAYERS_FEATURES where ID_LAYER = ? and ID_FEATURE = ?", capa, id);
    }

    private JsonNode jsonGuardado(int capa, int id) {
        return JSON.readTree(jdbc.queryForObject("select JSON_METADATA from LAYERS_FEATURES where ID_LAYER = ? and ID_FEATURE = ?",
                String.class, capa, id));
    }

    private String espacial(int capa, int id) {
        return jdbc.queryForObject("select SPATIAL_OBJECT from LAYERS_FEATURES where ID_LAYER = ? and ID_FEATURE = ?",
                String.class, capa, id);
    }

    private int filasDe(int capa) {
        return jdbc.queryForObject("select count(*) from LAYERS_FEATURES where ID_LAYER = ?", Integer.class, capa);
    }

    @Test
    void enUnaCapaEnBlancoNumeraDesdeCeroComoSiate() {
        assertThat(repositorio.guardar(CAPA, YO, nuevos(punto(-1, -70.65, -33.44), linea(-2)))).contains(List.of(0, 1));

        assertThat(filaGuardada(CAPA, 0)).containsEntry("TYPE", "Point");
        assertThat(filaGuardada(CAPA, 1)).containsEntry("TYPE", "LineString");
        assertThat(filaGuardada(CAPA, 1).get("DATE_CREATE")).isNotNull();
        assertThat(filaGuardada(CAPA, 1).get("DATE_MODIFY")).isNotNull();
    }

    @Test
    void conFilasContinuaDesdeElMayorIdAunqueHayaHuecos() {
        fila(CAPA, 0, "{}");
        fila(CAPA, 7, "{}");

        assertThat(repositorio.guardar(CAPA, YO, nuevos(punto(-1, -70.65, -33.44)))).contains(List.of(8));
    }

    @Test
    void escribeElJsonQueLeeSiateConSuGeometriaEnGrados() {
        repositorio.guardar(CAPA, YO, nuevos(linea(-1)));

        JsonNode json = jsonGuardado(CAPA, 0);
        assertThat(json.get("id").asString()).isEqualTo(CAPA + "-" + (CAPA * 100000L) + "0");
        assertThat(json.get("type").asString()).isEqualTo("Feature");
        assertThat(json.at("/geometry/type").asString()).isEqualTo("LineString");
        assertThat(json.at("/geometry/coordinates/1/0").asDouble()).isEqualTo(-70.5);
        assertThat(json.at("/geometry/creationdate").asString()).matches("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}");
        assertThat(json.at("/properties/color").asString()).isEqualTo("#ff0000");
        assertThat(json.at("/properties/width").asDouble()).isEqualTo(3);
        assertThat(json.at("/properties/fill").asString()).isEqualTo("rgba(1,2,3,0.5)");
        assertThat(json.at("/metadata/name").asString()).isEqualTo("Ruta");
        assertThat(json.at("/metadata/description").asString()).isEqualTo("A la bodega");
        assertThat(json.at("/metadata/metadata")).hasSize(1);
        assertThat(json.at("/metadata/metadata/0/name").asString()).isEqualTo("Fuente");
        assertThat(json.at("/metadata/metadata/0/description").asString()).isEqualTo("Terreno");
    }

    @Test
    void loEscritoSeVuelveALeerIgual() {
        repositorio.guardar(CAPA, YO, nuevos(linea(-1)));

        ElementoCapa leido = ElementoSiate.leer(0, jsonGuardado(CAPA, 0).toString()).orElseThrow();

        assertThat(leido.geometria()).isEqualTo(linea(-1).geometria());
        assertThat(leido.estilo()).isEqualTo(LINEA);
        assertThat(leido.metadatos()).containsExactly(new Metadato("Fuente", "Terreno"));
    }

    @Test
    void laGeometriaEspacialVaEnEpsg4326() {
        repositorio.guardar(CAPA, YO, nuevos(punto(-1, -70.65, -33.44)));

        assertThat(espacial(CAPA, 0)).isEqualTo("4326:POINT (-70.65 -33.44)");
    }

    @Test
    void unCirculoGuardaCentroYRadioEnElJsonYUnPoligonoEnLaGeometriaEspacial() {
        ElementoCapa circulo = new ElementoCapa(-1, Geometria.simple("Point", List.of(-70.65, -33.44)), 500, "", "",
                EstiloElemento.SIN_ESTILO, List.of());

        repositorio.guardar(CAPA, YO, nuevos(circulo));

        JsonNode json = jsonGuardado(CAPA, 0);
        assertThat(filaGuardada(CAPA, 0)).containsEntry("TYPE", "Circle");
        assertThat(json.at("/geometry/type").asString()).isEqualTo("Circle");
        assertThat(json.at("/geometry/center/0").asDouble()).isEqualTo(-70.65);
        assertThat(json.at("/geometry/radius").asDouble()).isEqualTo(500);
        assertThat(json.at("/geometry/coordinates").isMissingNode()).isTrue();
        assertThat(espacial(CAPA, 0)).startsWith("4326:POLYGON ((");
    }

    @Test
    void modificarConservaLoQueNoSeEditaYSoloCambiaLaFechaDeModificacion() {
        fila(CAPA, 3, """
                {"id":"antiguo","type":"Feature","overlayruta":{"leyenda":"F1"},
                 "geometry":{"type":"Point","coordinates":[-1.0,-1.0],"creationdate":"01-01-2019 10:00:00"},
                 "properties":{"image":"./assets/img/marker.svg","extra":1},
                 "metadata":{"code":"x","name":"Viejo"}}""");

        assertThat(repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(), List.of(punto(3, -70.65, -33.44)), List.of())))
                .contains(List.of());

        JsonNode json = jsonGuardado(CAPA, 3);
        assertThat(json.get("id").asString()).isEqualTo("antiguo");
        assertThat(json.at("/overlayruta/leyenda").asString()).isEqualTo("F1");
        assertThat(json.at("/geometry/coordinates/0").asDouble()).isEqualTo(-70.65);
        assertThat(json.at("/geometry/creationdate").asString()).isEqualTo("01-01-2019 10:00:00");
        assertThat(json.at("/properties/image").asString()).isEqualTo("./assets/img/marker.svg");
        assertThat(json.at("/properties/extra").asInt()).isEqualTo(1);
        assertThat(json.at("/metadata/code").asString()).isEqualTo("x");
        assertThat(json.at("/metadata/name").asString()).isEqualTo("Punto");
        assertThat(espacial(CAPA, 3)).isEqualTo("4326:POINT (-70.65 -33.44)");
        assertThat(filaGuardada(CAPA, 3)).containsEntry("TYPE", "Point");
        assertThat(filaGuardada(CAPA, 3).get("DATE_CREATE").toString()).startsWith("2020-01-02");
        assertThat(filaGuardada(CAPA, 3).get("DATE_MODIFY")).isNotNull();
    }

    @Test
    void quitarElEstiloVuelveAlDeLaAplicacion() {
        fila(CAPA, 3, """
                {"geometry":{"type":"LineString","coordinates":[[-1.0,-1.0],[-2.0,-2.0]]},
                 "properties":{"color":"#000000","width":9,"fill":"#111111"}}""");
        ElementoCapa sinEstilo = new ElementoCapa(3, linea(0).geometria(), 0, "", "", EstiloElemento.SIN_ESTILO, List.of());

        repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(), List.of(sinEstilo), List.of()));

        assertThat(jsonGuardado(CAPA, 3).get("properties")).isEmpty();
    }

    @Test
    void modificarUnElementoQueYaNoExisteRechazaElGuardado() {
        assertThatThrownBy(() -> repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(), List.of(punto(3, -70.65, -33.44)), List.of())))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("ya no existe");
    }

    /** Sin la transacción del test: la del repositorio tiene que abarcar lo que hace JPA y lo que hace JdbcTemplate. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void siFallaUnCambioNoQuedaNingunoAMedias() {
        fila(CAPA, 0, "{}");

        assertThatThrownBy(() -> repositorio.guardar(CAPA, YO,
                new CambiosElementos(List.of(), List.of(punto(9, -70.65, -33.44)), List.of(0))))
                .isInstanceOf(SolicitudInvalidaException.class);

        assertThat(filasDe(CAPA)).isEqualTo(1);
    }

    @Test
    void unaGeometriaInvalidaRechazaElGuardado() {
        assertThatThrownBy(() -> repositorio.guardar(CAPA, YO, nuevos(punto(-1, 500, 500))))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void eliminaSoloDeEsaCapaEIgnoraLoQueYaNoEsta() {
        capa(51, YO, "0", "1", null);
        fila(CAPA, 0, "{}");
        fila(CAPA, 1, "{}");
        fila(51, 0, "{}");

        assertThat(repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(), List.of(), List.of(0, 9)))).contains(List.of());

        assertThat(filasDe(CAPA)).isEqualTo(1);
        assertThat(filasDe(51)).isEqualTo(1);
    }

    @Test
    void aplicaTodoEnElMismoGuardado() {
        fila(CAPA, 0, "{}");
        fila(CAPA, 1, "{}");

        assertThat(repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(linea(-1)), List.of(punto(1, -70.1, -33.1)), List.of(0))))
                .contains(List.of(2));

        assertThat(jdbc.queryForList("select ID_FEATURE from LAYERS_FEATURES where ID_LAYER = ? order by ID_FEATURE", Integer.class, CAPA))
                .containsExactly(1, 2);
        assertThat(espacial(CAPA, 1)).isEqualTo("4326:POINT (-70.1 -33.1)");
    }

    @Test
    void noSeDibujaEnUnaCapaAjenaInactivaFormalODeUnGrupo() {
        capa(51, OTRO, "1", "1", null);
        capa(52, YO, "0", "0", null);
        capa(53, YO, "2", "1", null);
        capa(54, YO, "3", "1", null);

        for (int capa : List.of(51, 52, 53, 54, 99)) {
            assertThat(repositorio.guardar(capa, YO, nuevos(punto(-1, -70.65, -33.44)))).isEmpty();
            assertThat(filasDe(capa)).isZero();
        }
    }

    @Test
    void noSeDibujaEnUnaCapaPorUrlNiEnUnaColeccionTraidaDeUnaUrl() {
        capa(51, YO, "0", "1", "{\"type\":\"WMS\",\"url\":\"https://ide.cl/wms\"}");
        capa(52, YO, "0", "1", "{\"type\":\"FeatureCollection\",\"zoom\":4,\"url\":\"https://ide.cl/wfs\",\"features\":[]}");

        assertThat(repositorio.guardar(51, YO, nuevos(punto(-1, -70.65, -33.44)))).isEmpty();
        assertThat(repositorio.guardar(52, YO, nuevos(punto(-1, -70.65, -33.44)))).isEmpty();
    }

    @Test
    void unaCapaSinVisibilidadSeEditaComoPrivada() {
        capa(51, YO, null, "1", null);

        assertThat(repositorio.guardar(51, YO, nuevos(punto(-1, -70.65, -33.44)))).contains(List.of(0));
    }

    @Test
    void elPrimerGuardadoDeUnaCapaConSusElementosEnElJsonLosPasaAFilasConSuPosicionComoId() {
        String json = """
                {"type":"FeatureCollection","features":[
                 {"type":"Feature","geometry":{"type":"Point","coordinates":[-70.65,-33.44]},"metadata":{"name":"A"}},
                 {"type":"Feature","geometry":{"type":"Point"}},
                 {"type":"Feature","geometry":{"type":"MultiPolygon","coordinates":[[[[-70.0,-33.0],[-70.1,-33.0],[-70.1,-33.1],[-70.0,-33.0]]]]}},
                 {"type":"Feature","geometry":{"type":"Circle","center":[-70.0,-33.0],"radius":100.5}}]}""";
        capa(51, YO, "0", "1", json);

        assertThat(repositorio.guardar(51, YO, nuevos(punto(-1, -70.1, -33.1)))).contains(List.of(4));

        assertThat(jdbc.queryForList("select ID_FEATURE from LAYERS_FEATURES where ID_LAYER = 51 order by ID_FEATURE", Integer.class))
                .containsExactly(0, 2, 3, 4);
        assertThat(jsonGuardado(51, 0).at("/metadata/name").asString()).isEqualTo("A");
        assertThat(filaGuardada(51, 2)).containsEntry("TYPE", "Collection");
        assertThat(espacial(51, 2)).startsWith("4326:MULTIPOLYGON");
        assertThat(filaGuardada(51, 3)).containsEntry("TYPE", "Circle");
        assertThat(jdbc.queryForObject("select JSON_METADATA from LAYERS where ID = 51", String.class)).isEqualTo(json);
    }

    @Test
    void modificarConUnIconoLoGuardaEnImageComoSiate() {
        fila(CAPA, 3, """
                {"geometry":{"type":"Point","coordinates":[-1.0,-1.0]},"properties":{"image":"./assets/img/marker.svg","extra":1}}""");
        ElementoCapa base = punto(3, -70.65, -33.44);
        ElementoCapa conIcono = new ElementoCapa(base.id(), base.geometria(), base.radio(), base.nombre(), base.descripcion(),
                new EstiloElemento("", 0, "", "data:image/png;base64,AA=="), base.metadatos());

        repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(), List.of(conIcono), List.of()));

        JsonNode json = jsonGuardado(CAPA, 3);
        assertThat(json.at("/properties/image").asString()).isEqualTo("data:image/png;base64,AA==");
        assertThat(json.at("/properties/extra").asInt()).isEqualTo(1);
    }

    @Test
    void modificarYEliminarFuncionanEnElMismoGuardadoQueReparteLaColeccion() {
        capa(51, YO, "0", "1", """
                {"type":"FeatureCollection","features":[
                 {"geometry":{"type":"Point","coordinates":[-70.65,-33.44]},"properties":{"image":"x.svg"}},
                 {"geometry":{"type":"Point","coordinates":[-70.0,-33.0]}}]}""");

        repositorio.guardar(51, YO, new CambiosElementos(List.of(), List.of(punto(0, -71.0, -34.0)), List.of(1)));

        assertThat(filasDe(51)).isEqualTo(1);
        assertThat(jsonGuardado(51, 0).at("/geometry/coordinates/0").asDouble()).isEqualTo(-71.0);
        assertThat(jsonGuardado(51, 0).at("/properties/image").asString()).isEqualTo("x.svg");
    }

    @Test
    void unElementoDelJsonQueOracleSpatialNoAceptaSeConservaSinGeometriaEspacial() {
        capa(51, YO, "0", "1", """
                {"type":"FeatureCollection","features":[
                 {"geometry":{"type":"Polygon","coordinates":[[[-70.0,-33.0],[-70.1,-33.0],[-70.1,-33.1],[-70.2,-33.2]]]}}]}""");

        repositorio.guardar(51, YO, nuevos(punto(-1, -70.1, -33.1)));

        assertThat(filasDe(51)).isEqualTo(2);
        assertThat(espacial(51, 0)).isNull();
        assertThat(espacial(51, 1)).isNotNull();
    }

    @Test
    void unaColeccionVaciaNoCreaFilasYElPrimerElementoEsElCero() {
        capa(51, YO, "0", "1", "{\"type\":\"FeatureCollection\",\"features\":[]}");

        assertThat(repositorio.guardar(51, YO, nuevos(punto(-1, -70.1, -33.1)))).contains(List.of(0));
    }

    @Test
    void laAlturaDeUnArchivoNoLlegaALaGeometriaEspacial() {
        ElementoCapa conAltura = new ElementoCapa(-1, Geometria.simple("LineString",
                List.of(List.of(-70.6, -33.4, 120.0), List.of(-70.5, -33.3, 130.0))), 0, "", "", EstiloElemento.SIN_ESTILO, List.of());

        repositorio.guardar(CAPA, YO, nuevos(conAltura));

        assertThat(espacial(CAPA, 0)).isEqualTo("4326:LINESTRING (-70.6 -33.4, -70.5 -33.3)");
    }

    @Test
    void unaGeometriaLargaVaComoClobJuntoAOtrasCortas() {
        List<List<Double>> vertices = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            vertices.add(List.of(-70.123456789 - i * 0.001, -33.123456789 - i * 0.0005));
        }
        ElementoCapa larga = new ElementoCapa(-2, Geometria.simple("LineString", vertices), 0, "", "", EstiloElemento.SIN_ESTILO, List.of());

        assertThat(repositorio.guardar(CAPA, YO, nuevos(punto(-1, -70.65, -33.44), larga, punto(-3, -70.1, -33.1)))).contains(List.of(0, 1, 2));

        assertThat(espacial(CAPA, 0)).isEqualTo("4326:POINT (-70.65 -33.44)");
        assertThat(espacial(CAPA, 1)).startsWith("4326:LINESTRING").hasSizeGreaterThan(1000);
        assertThat(jsonGuardado(CAPA, 1).at("/geometry/coordinates")).hasSize(200);
        assertThat(espacial(CAPA, 2)).isEqualTo("4326:POINT (-70.1 -33.1)");
    }

    @Test
    void importarCreaUnaCapaPrivadaConSusElementosComoFilas() {
        int id = repositorio.importar(YO, new CapaImportada("Bodegas", "Región", OrigenArchivo.KML, List.of(punto(-1, -70.65, -33.44), linea(-2))));

        assertThat(jdbc.queryForMap("select * from LAYERS where ID = ?", id))
                .containsEntry("OWNER", YO).containsEntry("NAME", "Bodegas").containsEntry("DESCRIPTION", "Región")
                .containsEntry("STATUS", "1").containsEntry("VISIBILITY", "0").containsEntry("ORIGIN", "KML")
                .containsEntry("JSON_METADATA", null).containsEntry("CATEGORY_ID", null);
        assertThat(filaGuardada(id, 0)).containsEntry("TYPE", "Point");
        assertThat(filaGuardada(id, 1)).containsEntry("TYPE", "LineString");
        assertThat(espacial(id, 0)).isEqualTo("4326:POINT (-70.65 -33.44)");
        assertThat(jsonGuardado(id, 1).at("/metadata/name").asString()).isEqualTo("Ruta");
    }

    @Test
    void unaCapaDeUnArchivoJsonNoLlevaOrigenComoEnSiate() {
        int id = repositorio.importar(YO, new CapaImportada("Bodegas", "", OrigenArchivo.GEOJSON, List.of(punto(-1, -70.65, -33.44))));

        assertThat(jdbc.queryForMap("select * from LAYERS where ID = ?", id)).containsEntry("ORIGIN", null);
    }

    @Test
    void importaMasElementosQueUnLoteDeEscritura() {
        List<ElementoCapa> muchos = new ArrayList<>();
        for (int i = 0; i < 1201; i++) {
            muchos.add(punto(-1 - i, -70 - i * 0.001, -33));
        }

        int id = repositorio.importar(YO, new CapaImportada("Bodegas", "", OrigenArchivo.GEOJSON, muchos));

        assertThat(filasDe(id)).isEqualTo(1201);
        assertThat(filaGuardada(id, 1200)).isNotNull();
    }

    /** Sin la transacción del test: si un elemento no se puede guardar, tampoco queda la capa. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void siUnElementoNoSePuedeGuardarNoQuedaLaCapa() {
        int antes = jdbc.queryForObject("select count(*) from LAYERS", Integer.class);

        assertThatThrownBy(() -> repositorio.importar(YO, new CapaImportada("Bodegas", "", OrigenArchivo.KML,
                List.of(punto(-1, -70.65, -33.44), punto(-2, 500, 500)))))
                .isInstanceOf(SolicitudInvalidaException.class);

        assertThat(jdbc.queryForObject("select count(*) from LAYERS", Integer.class)).isEqualTo(antes);
    }

    @Test
    void conFilasNoVuelveARepartirLaColeccion() {
        capa(51, YO, "0", "1", """
                {"type":"FeatureCollection","features":[{"geometry":{"type":"Point","coordinates":[-1.0,-1.0]}}]}""");
        fila(51, 5, "{}");

        assertThat(repositorio.guardar(51, YO, nuevos(punto(-1, -70.1, -33.1)))).contains(List.of(6));

        assertThat(filasDe(51)).isEqualTo(2);
    }
}
