package siante2;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.TipoCapa;
import siante2.categorias.application.port.out.CategoriasReadPort;
import siante2.categorias.domain.Categoria;
import siante2.multimedia.application.port.out.MultimediaReadPort;
import siante2.usuarios.application.port.out.UsuariosPort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Los adaptadores de hoy, escritos para Oracle, sobre PostgreSQL sin cambios: lo que falla aquí es lo que hay que
 * adaptar para pasar de base (docs/modelo-datos.md). Los datos se cargan como los guarda SIATE. */
@Tag("postgres")
@SpringBootTest(properties = {PostgresDePrueba.FLYWAY, PostgresDePrueba.MIGRACIONES})
@Import(PostgresDePrueba.class)
class AdaptadoresPostgresTest {

    private static final String PUNTO = "{\"geometry\":{\"type\":\"Point\",\"coordinates\":[-70.65,-33.44]},"
            + "\"metadata\":{\"name\":\"Bodega\"}}";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UsuariosPort usuarios;

    @Autowired
    private CategoriasReadPort categorias;

    @Autowired
    private CapasReadPort capas;

    @Autowired
    private MultimediaReadPort multimedia;

    @BeforeEach
    void limpiar() {
        jdbc.execute("truncate users, categories, layers, layers_features, layers_groups, layers_groups_layers, favorites,"
                + " multimedia restart identity cascade");
    }

    @Test
    void registraUnUsuarioConIdPorIdentityYLoEncuentra() {
        usuarios.registrar("jperez", List.of("SIATE", "CGR"));

        var usuario = usuarios.buscarPorIvUser("jperez", List.of("SIATE")).orElseThrow();

        assertThat(usuario.id()).isPositive();
        assertThat(usuario.nombre()).isEqualTo("jperez");
        assertThat(jdbc.queryForObject("select iv_group from users where iv_user = 'jperez'", String.class))
                .isEqualTo("SIATE,CGR");
    }

    @Test
    void listaLasCategoriasActivas() {
        jdbc.update("insert into categories (id, name, description, status) values (1, 'Salud', '', '1'), (2, 'Vieja', '', '0')");

        assertThat(categorias.buscarActivas()).extracting(Categoria::nombre).containsExactly("Salud");
    }

    @Test
    void listaLasCapasDelUsuarioYAbreUnaConSusElementosDesdeLaVista() {
        jdbc.update("insert into users (id, iv_user) values (7, 'jperez')");
        jdbc.update("insert into layers (id, owner, name, status, visibility, json_metadata) values"
                + " (1, 7, 'Bodegas', '1', '0', '{\"type\":\"FeatureCollection\",\"features\":[]}'),"
                + " (2, 7, 'Servicio IDE', '1', '1', '{\"type\":\"WMS\",\"url\":\"https://ide.cl/wms\"}')");
        // Mismo ID_FEATURE en dos capas: SIATE los numera dentro de cada una.
        // La geometría va como GeoJSON en json_metadata, de donde la lee siante2 (sin PostGIS).
        jdbc.update("insert into layers_features (id_feature, id_layer, type, json_metadata) values"
                + " (0, 1, 'Point', ?), (0, 2, 'Point', ?)", PUNTO, PUNTO);

        assertThat(capas.listarDe(7)).extracting(r -> r.nombre()).containsExactly("Bodegas", "Servicio IDE");
        Capa capa = capas.buscarVisible(1, 7).orElseThrow();
        assertThat(capa.tipo()).isEqualTo(TipoCapa.VECTORIAL);
        assertThat(capa.elementos()).extracting(ElementoCapa::nombre).containsExactly("Bodega");
        assertThat(capas.buscarVisible(2, 7).orElseThrow().tipo()).isEqualTo(TipoCapa.URL);
    }

    @Test
    void listaLasFavoritasYElCatalogoConLasClavesForaneasDeOracle() {
        jdbc.update("insert into users (id, iv_user, name, last_name) values (1, 'siante', 'SIANTE', ''), (7, 'jperez', 'Juan', 'Pérez')");
        jdbc.update("insert into categories (id, name) values (12, 'Salud')");
        jdbc.update("insert into layers (id, owner, name, status, visibility, category_id) values"
                + " (3, 7, 'Ríos', '1', '1', 12), (5, 1, 'Hospitales', '1', '2', 12)");
        // El favorito apunta a un grupo y las capas salen de layers_groups_layers, que en PostgreSQL exige que existan.
        jdbc.update("insert into layers_groups (id, owner) values (10, 7), (11, 1)");
        jdbc.update("insert into layers_groups_layers (l_group_id, layer_id) values (10, 3), (11, 5)");
        jdbc.update("insert into favorites (user_id, l_group_id) values (7, 10), (7, 11)");

        assertThat(capas.listarFavoritasDe(7)).extracting(ResumenCapa::nombre).containsExactlyInAnyOrder("Ríos", "Hospitales");
        assertThat(capas.catalogo(SeccionCatalogo.PUBLICAS, 7))
                .extracting(CapaCatalogo::nombre, CapaCatalogo::dueno, CapaCatalogo::categoria, CapaCatalogo::favorita)
                .containsExactly(tuple("Ríos", "Juan Pérez", "Salud", true));
        assertThat(capas.catalogo(SeccionCatalogo.FORMALES, 7)).extracting(CapaCatalogo::nombre).containsExactly("Hospitales");
    }

    @Test
    void unaCapaConJsonVacioNoRompeLaLectura() {
        // Oracle guarda '' como NULL; PostgreSQL guarda '' tal cual.
        jdbc.update("insert into users (id, iv_user) values (7, 'jperez')");
        jdbc.update("insert into layers (id, owner, name, status, visibility, json_metadata) values (1, 7, 'Vacía', '1', '0', '')");

        assertThat(capas.buscarVisible(1, 7)).hasValueSatisfying(capa -> assertThat(capa.elementos()).isEmpty());
    }

    @Test
    @Disabled("Brecha: con @Lob, Hibernate lee MULTIMEDIA.ARCHIVO como oid y en PostgreSQL es bytea. Se resuelve en el"
            + " adaptador de PostgreSQL (docs/modelo-datos.md, \"Prueba de concepto\")")
    void leeLaMultimediaYSuArchivo() {
        jdbc.update("insert into multimedia (x_multimedia, descripcion, archivo) values (1, 'Manual', ?)",
                (Object) new byte[] {1, 2, 3});

        assertThat(multimedia.listar()).hasSize(1);
        assertThat(multimedia.contenido(1)).hasValueSatisfying(c -> assertThat(c.bytes()).containsExactly(1, 2, 3));
    }

    @Test
    void losCorrelativosDeReportesAvanzanEnCadaLectura() {
        Integer primero = jdbc.queryForObject("select correlativo from nextval_public_reports", Integer.class);
        Integer segundo = jdbc.queryForObject("select correlativo from nextval_public_reports", Integer.class);

        assertThat(segundo).isEqualTo(primero + 1);
    }
}
