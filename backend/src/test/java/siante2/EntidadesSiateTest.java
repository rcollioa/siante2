package siante2;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** Las entidades usan exactamente las tablas y columnas de los modelos de SIATE
 * ({@code backend/src/siante/model}); se excluyen a propósito FILE_BASE64 y SPATIAL_OBJECT. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class EntidadesSiateTest {

    static final Map<String, List<String>> COLUMNAS_SIATE = Map.ofEntries(
            Map.entry("CATEGORIES", List.of("ID", "NAME", "DESCRIPTION", "STATUS")),
            Map.entry("USERS", List.of("ID", "NAME", "LAST_NAME", "EMAIL", "AREA", "IV_USER", "IV_GROUP")),
            Map.entry("LAYERS", List.of("ID", "OWNER", "NAME", "ORIGIN", "STATUS", "JSON_METADATA", "DESCRIPTION",
                    "VISIBILITY", "L_GROUP_ID", "CATEGORY_ID")),
            Map.entry("LAYERS_CATEGORIES", List.of("LAYER_ID", "CATEGORY_ID")),
            Map.entry("LAYERS_FEATURES", List.of("ID_FEATURE", "ID_LAYER", "TYPE", "DATE_CREATE", "DATE_MODIFY",
                    "JSON_METADATA")),
            Map.entry("POINTS_CATALOG", List.of("ID", "NAME", "URL", "NAME_FILE", "VISIBILITY", "OWNER", "NAME_IMAGE")),
            Map.entry("V_LAYERS", List.of("ID", "NAME", "ORIGIN", "JSON_METADATA", "DESCRIPTION", "VISIBILITY", "STATUS",
                    "USER_ID", "USER_NAME", "USER_LAST_NAME", "USER_EMAIL", "USER_AREA", "CATEGORY_ID", "CATEGORY_NAME")),
            Map.entry("SCHEDULER_DATA", List.of("ID", "DATA", "STATUS", "LAYER_ID", "L_GROUP_ID")),
            Map.entry("V_LF_TYPE_FILTER", List.of("ID_FEATURE", "ID_LAYER", "JSON_METADATA")),
            Map.entry("V_SELECT_AREA", List.of("ID", "NAME", "OWNER", "VISIBILITY")),
            Map.entry("V_MEASURE_DISTANCE", List.of("ID_FEATURE_BASE", "ID_LAYER_BASE", "JSON_BASE", "ID_FEATURE_COMP",
                    "ID_LAYER_COMP", "JSON_COMP", "DISTANCE")),
            Map.entry("FAVORITES", List.of("USER_ID", "L_GROUP_ID", "CREATED_DATE")),
            Map.entry("LAYERS_GROUPS", List.of("ID", "NAME", "DESCRIPTION", "OWNER", "STATUS", "CREATED_DATE",
                    "IS_SCHEDULER", "CREATE_TYPE")),
            Map.entry("LAYERS_GROUPS_LAYERS", List.of("LAYER_ID", "L_GROUP_ID", "CREATED_DATE")),
            Map.entry("V_LAYERS_GROUPS", List.of("ID", "NAME", "DESCRIPTION", "OWNER", "LAYERS_COUNT", "STATUS",
                    "IS_SCHEDULER", "CREATE_TYPE")),
            Map.entry("PUBLIC_REPORTS", List.of("CREADO", "F_CREACION", "X_PURE", "VERSION", "LAYER_ID",
                    "LAYERS_GROUPS_ID", "QR", "NAME", "JSON_METADATA", "DESCRIPCION")),
            Map.entry("PUBLIC_REPORTS_DETAILS", List.of("CREADO", "F_CREACION", "X_PUREDET", "PURE_X_PURE", "LAYER_ID",
                    "VERSION", "QR", "NAME", "JSON_METADATA")),
            Map.entry("NEXTVAL_PUBLIC_REPORTS", List.of("CORRELATIVO")),
            Map.entry("NEXTVAL_PUBLIC_REPORTS_DETAILS", List.of("CORRELATIVO")),
            Map.entry("MULTIMEDIA", List.of("CREADO", "F_CREACION", "MODIFICADO", "F_MODIFICACION", "DESCRIPCION",
                    "X_MULTIMEDIA", "ARCHIVO")));

    /** Claves primarias de las tablas en Oracle (diccionario del esquema SIANTE, docs/modelo-datos.md). Si la
     * entidad declara otra, Hibernate mezcla o descarta filas sin aviso. Las vistas no tienen clave y quedan fuera. */
    static final Map<String, List<String>> CLAVES_ORACLE = Map.ofEntries(
            Map.entry("CATEGORIES", List.of("ID")),
            Map.entry("USERS", List.of("ID")),
            Map.entry("LAYERS", List.of("ID")),
            Map.entry("LAYERS_CATEGORIES", List.of("LAYER_ID", "CATEGORY_ID")),
            Map.entry("LAYERS_FEATURES", List.of("ID_FEATURE", "ID_LAYER")),
            Map.entry("POINTS_CATALOG", List.of("ID")),
            Map.entry("SCHEDULER_DATA", List.of("ID")),
            Map.entry("FAVORITES", List.of("USER_ID", "L_GROUP_ID")),
            Map.entry("LAYERS_GROUPS", List.of("ID")),
            Map.entry("LAYERS_GROUPS_LAYERS", List.of("LAYER_ID", "L_GROUP_ID")),
            Map.entry("PUBLIC_REPORTS", List.of("X_PURE")),
            Map.entry("PUBLIC_REPORTS_DETAILS", List.of("X_PUREDET")),
            Map.entry("MULTIMEDIA", List.of("X_MULTIMEDIA")));

    @Autowired
    private JdbcTemplate jdbc;

    static Stream<Arguments> clavesOracle() {
        return CLAVES_ORACLE.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    static Stream<Arguments> tablas() {
        return COLUMNAS_SIATE.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    void laEntidadMapeaLasColumnasDelModelo(String tabla, List<String> columnas) {
        List<String> creadas = jdbc.queryForList(
                "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = 'PUBLIC' and TABLE_NAME = ?",
                String.class, tabla);

        assertThat(creadas).as("columnas de %s", tabla).containsExactlyInAnyOrderElementsOf(columnas);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("clavesOracle")
    void laClavePrimariaEsLaDeOracle(String tabla, List<String> clave) {
        List<String> creada = jdbc.queryForList("""
                select k.COLUMN_NAME from INFORMATION_SCHEMA.TABLE_CONSTRAINTS t
                join INFORMATION_SCHEMA.KEY_COLUMN_USAGE k
                  on k.CONSTRAINT_SCHEMA = t.CONSTRAINT_SCHEMA and k.CONSTRAINT_NAME = t.CONSTRAINT_NAME
                where t.TABLE_SCHEMA = 'PUBLIC' and t.TABLE_NAME = ? and t.CONSTRAINT_TYPE = 'PRIMARY KEY'""",
                String.class, tabla);

        assertThat(creada).as("clave primaria de %s", tabla).containsExactlyInAnyOrderElementsOf(clave);
    }
}
