package siante2.capas.adapter.out.oracle;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.ContadorSql;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.Visibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Favoritas y catálogo sobre V_LAYERS, FAVORITES y LAYERS_GROUPS_LAYERS en H2, con el usuario de capas formales
 * de SIATE como 1. */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "app.capas.usuario-formales=1"})
@Import(OracleCapasRepository.class)
class OracleCapasRepositoryCatalogoTest {

    private static final int SIANTE = 1;
    private static final int YO = 7;
    private static final int OTRO = 8;

    @Autowired
    private OracleCapasRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory fabrica;

    @BeforeEach
    void preparar() {
        jdbc.update("delete from V_LAYERS");
        jdbc.update("delete from FAVORITES");
        jdbc.update("delete from LAYERS_GROUPS_LAYERS");
        capa(1, YO, "Bodegas", "0", "1");
        capa(2, YO, "Área de un grupo", "3", "1");
        capa(3, OTRO, "Ríos", "1", "1");
        capa(4, OTRO, "Privada ajena", "0", "1");
        capa(5, SIANTE, "Hospitales", "2", "1");
        capa(6, SIANTE, "Borrador de SIANTE", "0", "1");
        capa(7, OTRO, "Pública borrada", "1", "0");
        // Como en SIATE, el favorito apunta a un grupo y las capas salen de LAYERS_GROUPS_LAYERS.
        grupo(10, 3, 4);
        grupo(11, 5);
        grupo(12, 2);
        grupo(13, 7);
        grupo(14, 1);
        favorito(YO, 10, 11, 12, 13);
        favorito(OTRO, 14);
    }

    @Test
    void lasFavoritasSonLasQueTodaviaPuedeVerSinAreasDeDibujoNiBorradasEnUnaSolaConsulta() {
        ContadorSql sql = new ContadorSql(fabrica);

        assertThat(repositorio.listarFavoritasDe(YO))
                .extracting(ResumenCapa::id, ResumenCapa::visibilidad)
                .containsExactly(tuple(3, Visibilidad.PUBLICA), tuple(5, Visibilidad.FORMAL));
        assertThat(sql.sentencias()).isEqualTo(1);
    }

    @Test
    void lasFavoritasDeOtroUsuarioNoSeMezclan() {
        assertThat(repositorio.listarFavoritasDe(OTRO)).isEmpty();
    }

    @Test
    void todasSonLasPublicasActivasConSuDuenoYCategoria() {
        ContadorSql sql = new ContadorSql(fabrica);

        assertThat(repositorio.catalogo(SeccionCatalogo.PUBLICAS, YO))
                .extracting(CapaCatalogo::id, CapaCatalogo::dueno, CapaCatalogo::categoria, CapaCatalogo::favorita)
                .containsExactly(tuple(3, "Ana Pérez", "Agua", true));
        assertThat(sql.sentencias()).isEqualTo(1);
    }

    @Test
    void usuarioSonLasPropiasSinAreasDeDibujo() {
        assertThat(repositorio.catalogo(SeccionCatalogo.MIAS, YO))
                .extracting(CapaCatalogo::id, CapaCatalogo::favorita)
                .containsExactly(tuple(1, false));
    }

    @Test
    void formalesSonLasDelUsuarioSianteQueCualquieraPuedeVer() {
        assertThat(repositorio.catalogo(SeccionCatalogo.FORMALES, YO))
                .extracting(CapaCatalogo::id, CapaCatalogo::visibilidad, CapaCatalogo::favorita)
                .containsExactly(tuple(5, Visibilidad.FORMAL, true));
    }

    private void capa(int id, int dueno, String nombre, String visibilidad, String estado) {
        jdbc.update("insert into V_LAYERS (ID, USER_ID, USER_NAME, USER_LAST_NAME, CATEGORY_NAME, NAME, VISIBILITY, STATUS)"
                + " values (?, ?, ?, ?, ?, ?, ?, ?)", id, dueno, dueno == OTRO ? "Ana" : "Usuario", dueno == OTRO ? "Pérez" : "",
                "Agua", nombre, visibilidad, estado);
    }

    private void grupo(int grupo, int... capas) {
        for (int capa : capas) {
            jdbc.update("insert into LAYERS_GROUPS_LAYERS (L_GROUP_ID, LAYER_ID) values (?, ?)", grupo, capa);
        }
    }

    private void favorito(int usuario, int... grupos) {
        for (int grupo : grupos) {
            jdbc.update("insert into FAVORITES (USER_ID, L_GROUP_ID) values (?, ?)", usuario, grupo);
        }
    }
}
