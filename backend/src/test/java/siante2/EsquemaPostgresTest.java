package siante2;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** El esquema que Flyway crea en PostgreSQL (espejo de Oracle) tiene las tablas, columnas y claves que esperan las
 * entidades: las mismas listas que {@link EntidadesSiateTest} verifica en H2. */
@Tag("postgres")
@DataJpaTest(properties = {PostgresDePrueba.FLYWAY, PostgresDePrueba.MIGRACIONES})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresDePrueba.class)
class EsquemaPostgresTest {

    /** Vistas de las herramientas de análisis: sin definición hasta que esas herramientas tengan ticket. */
    private static final Set<String> VISTAS_PENDIENTES = Set.of("V_LF_TYPE_FILTER", "V_SELECT_AREA", "V_MEASURE_DISTANCE");

    @Autowired
    private JdbcTemplate jdbc;

    static Stream<Arguments> tablas() {
        return EntidadesSiateTest.COLUMNAS_SIATE.entrySet().stream()
                .filter(e -> !VISTAS_PENDIENTES.contains(e.getKey()))
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    static Stream<Arguments> claves() {
        return EntidadesSiateTest.CLAVES_ORACLE.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    void tieneLasColumnasDeLaEntidad(String tabla, List<String> columnas) {
        List<String> creadas = jdbc.queryForList(
                "select upper(column_name) from information_schema.columns where table_schema = 'public' and table_name = ?",
                String.class, tabla.toLowerCase());

        assertThat(creadas).as("columnas de %s", tabla).containsAll(columnas);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("claves")
    void tieneLaClavePrimariaDeOracle(String tabla, List<String> clave) {
        List<String> creada = jdbc.queryForList("""
                select upper(k.column_name) from information_schema.table_constraints t
                join information_schema.key_column_usage k
                  on k.constraint_schema = t.constraint_schema and k.constraint_name = t.constraint_name
                where t.table_schema = 'public' and t.table_name = ? and t.constraint_type = 'PRIMARY KEY'""",
                String.class, tabla.toLowerCase());

        assertThat(creada).as("clave primaria de %s", tabla).containsExactlyInAnyOrderElementsOf(clave);
    }

    @Test
    void noNecesitaPostGisPorqueLaGeometriaVaEnElJson() {
        assertThat(jdbc.queryForObject("select count(*) from pg_extension where extname = 'postgis'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'layers_features'", String.class))
                .contains("json_metadata")
                .doesNotContain("spatial_object");
    }
}
