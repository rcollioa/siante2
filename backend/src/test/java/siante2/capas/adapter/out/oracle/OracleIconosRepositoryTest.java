package siante2.capas.adapter.out.oracle;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.ContadorSql;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;

import static org.assertj.core.api.Assertions.assertThat;

/** POINTS_CATALOG en H2, con URL y NAME_FILE como CLOB igual que en Oracle. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(OracleIconosRepository.class)
class OracleIconosRepositoryTest {

    private static final int YO = 7;
    private static final int OTRO = 8;
    private static final int SISTEMA = 1;
    private static final String PNG = "data:image/png;base64,iVBORw0KGgo=";
    private static final String SVG = "data:image/svg+xml;base64,PHN2Zy8+";

    @Autowired
    private OracleIconosRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory fabrica;

    @BeforeEach
    void preparar() {
        jdbc.execute("alter table POINTS_CATALOG alter column URL clob");
        jdbc.execute("alter table POINTS_CATALOG alter column NAME_FILE clob");
        jdbc.update("delete from POINTS_CATALOG");
        fila(10, "Del sistema", "sistema.png", null, PNG, "1", SISTEMA);
        fila(11, "Mío", "mio.svg", null, SVG, "1", YO);
        fila(12, "Ajeno", "ajeno.png", null, PNG, "1", OTRO);
        fila(13, "Borrado", "borrado.png", null, PNG, "0", YO);
        fila(14, "Por URL", null, "https://otro.cl/icono.png", null, "1", YO);
        fila(15, null, null, null, "no es una imagen", "1", SISTEMA);
    }

    @Test
    void listaLosActivosDelSistemaYDelUsuarioConImagenEnUnaConsulta() {
        ContadorSql sql = new ContadorSql(fabrica);

        assertThat(repositorio.listar(YO)).containsExactly(
                new Icono(10, "Del sistema", "sistema.png", PNG, false),
                new Icono(11, "Mío", "mio.svg", SVG, true));
        assertThat(sql.sentencias()).isEqualTo(1);
    }

    @Test
    void sinUsuarioSoloVeLosDelSistema() {
        assertThat(repositorio.listar(-1)).extracting(Icono::id).containsExactly(10);
    }

    @Test
    void elUsuarioDelSistemaNoLosVeComoPropios() {
        assertThat(repositorio.listar(SISTEMA)).extracting(Icono::propio).containsExactly(false);
    }

    @Test
    void crearLoDejaActivoConSuDuenoYSinUrl() {
        int id = repositorio.crear(YO, new DatosIcono("Nuevo", "nuevo.png", PNG));

        assertThat(leer(id)).containsEntry("NAME", "Nuevo").containsEntry("NAME_IMAGE", "nuevo.png")
                .containsEntry("NAME_FILE", PNG).containsEntry("OWNER", YO).containsEntry("VISIBILITY", "1")
                .containsEntry("URL", null);
    }

    @Test
    void editarCambiaUnoPropioYDejaAtrasSuUrl() {
        ContadorSql sql = new ContadorSql(fabrica);

        assertThat(repositorio.actualizar(14, YO, new DatosIcono("Ya no URL", "a.png", PNG))).isTrue();

        assertThat(sql.sentencias()).isEqualTo(1);
        assertThat(leer(14)).containsEntry("NAME", "Ya no URL").containsEntry("NAME_FILE", PNG).containsEntry("URL", null);
    }

    @Test
    void noCambiaUnoAjenoDelSistemaNiBorrado() {
        DatosIcono datos = new DatosIcono("Cambio", "", PNG);

        assertThat(repositorio.actualizar(12, YO, datos)).isFalse();
        assertThat(repositorio.actualizar(10, YO, datos)).isFalse();
        assertThat(repositorio.actualizar(13, YO, datos)).isFalse();
        assertThat(repositorio.eliminar(12, YO)).isFalse();
        assertThat(repositorio.eliminar(10, YO)).isFalse();
        assertThat(repositorio.eliminar(13, YO)).isFalse();
        assertThat(leer(12)).containsEntry("NAME", "Ajeno").containsEntry("VISIBILITY", "1");
    }

    @Test
    void eliminarLoDejaInactivoSinBorrarlo() {
        assertThat(repositorio.eliminar(11, YO)).isTrue();

        assertThat(leer(11)).containsEntry("VISIBILITY", "0").containsEntry("NAME", "Mío").containsEntry("NAME_FILE", SVG);
    }

    private void fila(int id, String nombre, String archivo, String url, String imagen, String visibilidad, int dueno) {
        jdbc.update("insert into POINTS_CATALOG (ID, NAME, NAME_IMAGE, URL, NAME_FILE, VISIBILITY, OWNER) values (?, ?, ?, ?, ?, ?, ?)",
                id, nombre, archivo, url, imagen, visibilidad, dueno);
    }

    private Map<String, Object> leer(int id) {
        return jdbc.queryForMap("select * from POINTS_CATALOG where ID = ?", id);
    }
}
