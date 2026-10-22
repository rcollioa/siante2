package siante2.compartido.adapter.out.oracle;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;
import siante2.compartido.domain.Geometria;

import static org.assertj.core.api.Assertions.assertThat;

/** Escritura contra Oracle Free (mvnw test -Poracle): el WKT se guarda, Oracle lo valida y lo devuelve igual.
 * TO_GMLGEOMETRY requiere Java en la base, que Oracle Free no trae: la lectura se cubre en GmlAGeometriaTest. */
@Tag("oracle")
@Testcontainers
class GeometriaOracleTest {

    @Container
    private static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-free:23-faststart");

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void preparar() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(ORACLE.getJdbcUrl(), ORACLE.getUsername(), ORACLE.getPassword()));
        jdbc.execute("create table GEOMETRIAS (ID number primary key, G SDO_GEOMETRY)");
    }

    static Stream<Geometria> geometrias() {
        List<List<Double>> exterior = List.of(List.of(-70.7, -33.5), List.of(-70.6, -33.5), List.of(-70.6, -33.4),
                List.of(-70.7, -33.4), List.of(-70.7, -33.5));
        List<List<Double>> hueco = List.of(List.of(-70.68, -33.48), List.of(-70.66, -33.48), List.of(-70.66, -33.46),
                List.of(-70.68, -33.46), List.of(-70.68, -33.48));
        return Stream.of(
                Geometria.simple("Point", List.of(-70.65, -33.45)),
                Geometria.simple("LineString", List.of(List.of(-70.7, -33.5), List.of(-70.6, -33.4))),
                Geometria.simple("Polygon", List.of(exterior, hueco)),
                Geometria.simple("MultiPoint", List.of(List.of(-70.7, -33.5), List.of(-70.6, -33.4))),
                Geometria.simple("MultiPolygon", List.of(List.of(exterior), List.of(List.of(List.of(-70.5, -33.5),
                        List.of(-70.4, -33.5), List.of(-70.4, -33.4), List.of(-70.5, -33.5))))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("geometrias")
    void seGuardaYOracleLaValida(Geometria original) {
        int id = Math.abs(original.hashCode());
        jdbc.update("insert into GEOMETRIAS (ID, G) values (?, " + GeometriaAWkt.SQL_ESCRIBIR + ")",
                id, GeometriaAWkt.escribir(original));

        String valida = jdbc.queryForObject(
                "select SDO_GEOM.VALIDATE_GEOMETRY_WITH_CONTEXT(G, 0.005) from GEOMETRIAS where ID = ?", String.class, id);
        String guardada = jdbc.queryForObject(
                "select SDO_UTIL.TO_WKTGEOMETRY(G) from GEOMETRIAS where ID = ?", String.class, id);

        assertThat(valida).isEqualTo("TRUE");
        assertThat(guardada).isEqualTo(GeometriaAWkt.escribir(original));
    }
}
