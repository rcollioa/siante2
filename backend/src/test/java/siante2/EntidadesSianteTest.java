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

/** Las entidades usan exactamente las tablas y columnas de los modelos de SIANTE
 * ({@code backend/src/siante/model}); se excluyen a propósito FILE_BASE64 y SPATIAL_OBJECT. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class EntidadesSianteTest {

    private static final Map<String, List<String>> COLUMNAS_SIANTE = Map.ofEntries(
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
            Map.entry("MULTIMEDIA", List.of("CREADO", "F_CREACION", "MODIFICADO", "F_MODIFICACION", "DESCRIPCION",
                    "X_MULTIMEDIA")));

    @Autowired
    private JdbcTemplate jdbc;

    static Stream<Arguments> tablas() {
        return COLUMNAS_SIANTE.entrySet().stream().map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tablas")
    void laEntidadMapeaLasColumnasDelModelo(String tabla, List<String> columnas) {
        List<String> creadas = jdbc.queryForList(
                "select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA = 'PUBLIC' and TABLE_NAME = ?",
                String.class, tabla);

        assertThat(creadas).as("columnas de %s", tabla).containsExactlyInAnyOrderElementsOf(columnas);
    }
}
