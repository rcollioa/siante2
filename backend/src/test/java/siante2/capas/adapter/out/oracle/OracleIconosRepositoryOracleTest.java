package siante2.capas.adapter.out.oracle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.OracleFreeDePrueba;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;

import static org.assertj.core.api.Assertions.assertThat;

/** La imagen más grande que se acepta, en los CLOB de POINTS_CATALOG y con ID por trigger, como en SIATE. */
@Tag("oracle")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OracleFreeDePrueba.class, OracleIconosRepository.class})
class OracleIconosRepositoryOracleTest {

    private static final String GRANDE = "data:image/png;base64," + "a".repeat(31_976);

    @Autowired
    private OracleIconosRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararEsquemaDeSiante() {
        if (jdbc.queryForObject("select count(*) from user_tables where table_name = 'POINTS_CATALOG'", Integer.class) == 0) {
            jdbc.execute("create sequence SEQ_POINTS_CATALOG start with 500");
            jdbc.execute("create table POINTS_CATALOG (ID number primary key, NAME varchar2(200), URL clob, NAME_FILE clob,"
                    + " VISIBILITY varchar2(20), OWNER number, NAME_IMAGE varchar2(200))");
            jdbc.execute("create or replace trigger TRG_POINTS_CATALOG before insert on POINTS_CATALOG for each row"
                    + " begin :new.ID := SEQ_POINTS_CATALOG.nextval; end;");
        }
    }

    @Test
    void creaEditaYListaUnaImagenDelTamanoMaximo() {
        int id = repositorio.crear(7, new DatosIcono("Grande", "grande.png", GRANDE));
        assertThat(id).isGreaterThanOrEqualTo(500);
        assertThat(repositorio.listar(7)).contains(new Icono(id, "Grande", "grande.png", GRANDE, true));

        assertThat(repositorio.actualizar(id, 7, new DatosIcono("Otra", "", GRANDE))).isTrue();

        assertThat(jdbc.queryForObject("select count(*) from POINTS_CATALOG where ID = ? and NAME = 'Otra' and URL is null"
                + " and NAME_IMAGE is null and dbms_lob.getlength(NAME_FILE) = ?", Integer.class, id, GRANDE.length())).isEqualTo(1);
    }
}
