package siante2.capas.adapter.out.oracle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.OracleFreeDePrueba;
import siante2.capas.domain.CambiosElementos;
import siante2.capas.domain.CapaImportada;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.OrigenArchivo;
import siante2.compartido.domain.Geometria;

import static org.assertj.core.api.Assertions.assertThat;

/** LAYERS_FEATURES con SPATIAL_OBJECT y JSON_METADATA reales, igual que en SIATE. */
@Tag("oracle")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OracleFreeDePrueba.class, OracleElementosRepository.class})
class OracleElementosRepositoryOracleTest {

    private static final int YO = 7;
    private static final int CAPA = 50;

    @Autowired
    private OracleElementosRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararEsquemaDeSiante() {
        if (jdbc.queryForObject("select count(*) from user_tables where table_name = 'LAYERS'", Integer.class) == 0) {
            jdbc.execute("create table LAYERS (ID number primary key, OWNER number, NAME varchar2(200), ORIGIN varchar2(20),"
                    + " STATUS varchar2(20), JSON_METADATA clob, DESCRIPTION varchar2(1000), VISIBILITY varchar2(20),"
                    + " L_GROUP_ID number, CATEGORY_ID number)");
            jdbc.execute("create table LAYERS_FEATURES (ID_FEATURE number, ID_LAYER number, TYPE varchar2(100),"
                    + " DATE_CREATE date, DATE_MODIFY date, SPATIAL_OBJECT SDO_GEOMETRY, JSON_METADATA clob,"
                    + " primary key (ID_FEATURE, ID_LAYER))");
        }
        jdbc.update("delete from LAYERS_FEATURES");
        jdbc.update("delete from LAYERS");
        jdbc.update("insert into LAYERS (ID, OWNER, NAME, STATUS, VISIBILITY) values (?, ?, 'Capa', '1', '0')", CAPA, YO);
    }

    private static ElementoCapa elemento(int id, String tipo, List<?> coordenadas, double radio) {
        return new ElementoCapa(id, Geometria.simple(tipo, coordenadas), radio, "Nombre", "", EstiloElemento.SIN_ESTILO, List.of());
    }

    private String valida(int id) {
        return jdbc.queryForObject("select SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(SPATIAL_OBJECT, 0.005) from LAYERS_FEATURES"
                + " where ID_LAYER = ? and ID_FEATURE = ?", String.class, CAPA, id);
    }

    private String wkt(int id) {
        return jdbc.queryForObject("select SDO_UTIL.TO_WKTGEOMETRY(SPATIAL_OBJECT) from LAYERS_FEATURES"
                + " where ID_LAYER = ? and ID_FEATURE = ?", String.class, CAPA, id);
    }

    @Test
    void guardaLaGeometriaEspacialDeLoQueSeDibujaYOracleLaValida() {
        ElementoCapa punto = elemento(-1, "Point", List.of(-70.65, -33.44), 0);
        ElementoCapa linea = elemento(-2, "LineString", List.of(List.of(-70.6, -33.4), List.of(-70.5, -33.3)), 0);
        // En sentido horario, como puede dibujarlo el usuario: Oracle exige lo contrario.
        ElementoCapa cuadro = elemento(-3, "Polygon", List.of(List.of(List.of(-70.7, -33.5), List.of(-70.7, -33.4),
                List.of(-70.6, -33.4), List.of(-70.6, -33.5), List.of(-70.7, -33.5))), 0);
        ElementoCapa circulo = elemento(-4, "Point", List.of(-70.65, -33.44), 500);

        assertThat(repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(punto, linea, cuadro, circulo), List.of(), List.of())))
                .contains(List.of(0, 1, 2, 3));

        assertThat(List.of(valida(0), valida(1), valida(2), valida(3))).containsOnly("TRUE");
        assertThat(wkt(0)).startsWith("POINT");
        assertThat(wkt(3)).startsWith("POLYGON");
    }

    @Test
    void unaGeometriaDeMasDe4000BytesCabeEnElWkt() {
        List<List<Double>> vertices = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            vertices.add(List.of(-70.123456789 - i * 0.001, -33.123456789 - i * 0.0005));
        }

        assertThat(repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(elemento(-1, "LineString", vertices, 0)), List.of(), List.of())))
                .contains(List.of(0));

        assertThat(valida(0)).isEqualTo("TRUE");
    }

    @Test
    void modificarReemplazaLaGeometriaEspacialYElJson() {
        repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(elemento(-1, "Point", List.of(-70.65, -33.44), 0)), List.of(), List.of()));

        repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(), List.of(elemento(0, "Point", List.of(-71.5, -34.5), 0)), List.of()));

        assertThat(wkt(0)).contains("-71.5").contains("-34.5");
        assertThat(jdbc.queryForObject("select count(*) from LAYERS_FEATURES where ID_FEATURE = 0 and DATE_MODIFY is not null"
                + " and dbms_lob.instr(JSON_METADATA, '-71.5') > 0", Integer.class)).isEqualTo(1);
    }

    @Test
    void importaUnaCapaConElIdDelTriggerYSuGeometriaEspacial() {
        jdbc.execute("create sequence SEQ_LAYERS_IMPORTADAS start with 900");
        jdbc.execute("create or replace trigger TRG_LAYERS_IMPORTADAS before insert on LAYERS for each row"
                + " when (new.ID is null) begin :new.ID := SEQ_LAYERS_IMPORTADAS.nextval; end;");
        List<List<Double>> vertices = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            vertices.add(List.of(-70.123456789 - i * 0.001, -33.123456789 - i * 0.0005));
        }

        int id = repositorio.importar(YO, new CapaImportada("Bodegas", "", OrigenArchivo.KML, List.of(
                elemento(-1, "Point", List.of(-70.65, -33.44, 120.0), 0),
                elemento(-2, "LineString", vertices, 0))));

        assertThat(id).isGreaterThanOrEqualTo(900);
        assertThat(jdbc.queryForObject("select ORIGIN from LAYERS where ID = ?", String.class, id)).isEqualTo("KML");
        assertThat(jdbc.queryForList("select SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(SPATIAL_OBJECT, 0.005) from LAYERS_FEATURES"
                + " where ID_LAYER = ?", String.class, id)).containsExactly("TRUE", "TRUE");
    }

    @Test
    void reparteUnaColeccionGuardadaEnElJsonDeLaCapa() {
        jdbc.update("update LAYERS set JSON_METADATA = ? where ID = ?", """
                {"type":"FeatureCollection","features":[
                 {"geometry":{"type":"Point","coordinates":[-70.65,-33.44]}},
                 {"geometry":{"type":"MultiPoint","coordinates":[[-70.0,-33.0],[-70.1,-33.1]]}}]}""", CAPA);

        assertThat(repositorio.guardar(CAPA, YO, new CambiosElementos(List.of(elemento(-1, "Point", List.of(-70.5, -33.5), 0)), List.of(), List.of())))
                .contains(List.of(2));

        assertThat(List.of(valida(0), valida(1), valida(2))).containsOnly("TRUE");
    }
}
