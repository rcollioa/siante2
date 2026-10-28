package siante2.usuarios.adapter.out.oracle;

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

import static org.assertj.core.api.Assertions.assertThat;

/** Alta por JPA en USERS con ID asignado por trigger y secuencia, como en SIATE. */
@Tag("oracle")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleFreeDePrueba.class)
class OracleUsuariosRepositoryOracleTest {

    @Autowired
    private UsuarioJpaRepository jpa;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararEsquemaDeSiante() {
        if (jdbc.queryForObject("select count(*) from user_tables where table_name = 'USERS'", Integer.class) == 0) {
            jdbc.execute("create sequence SEQ_USERS start with 500");
            jdbc.execute("create table USERS (ID number primary key, NAME varchar2(50), LAST_NAME varchar2(50),"
                    + " EMAIL varchar2(100), AREA varchar2(50), IV_USER varchar2(20), IV_GROUP varchar2(1000))");
            jdbc.execute("create or replace trigger TRG_USERS before insert on USERS for each row"
                    + " begin :new.ID := SEQ_USERS.nextval; end;");
        }
    }

    @Test
    void registraConJpaYRecibeElIdDelTrigger() {
        OracleUsuariosRepository repositorio = new OracleUsuariosRepository(jpa);

        repositorio.registrar("jperez", List.of("SIATE", "CGR"));

        var usuario = repositorio.buscarPorIvUser("jperez", List.of("SIATE")).orElseThrow();
        assertThat(usuario.id()).isGreaterThanOrEqualTo(500);
        assertThat(usuario.nombre()).isEqualTo("jperez");
        assertThat(jdbc.queryForObject("select IV_GROUP from USERS where ID = ?", String.class, usuario.id()))
                .isEqualTo("SIATE,CGR");
    }
}
