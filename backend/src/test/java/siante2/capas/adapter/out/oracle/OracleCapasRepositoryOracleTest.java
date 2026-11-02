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
import siante2.capas.domain.NuevaCapa;

import static org.assertj.core.api.Assertions.assertThat;

/** Alta en LAYERS con ID asignado por trigger y secuencia, y JSON_METADATA como CLOB, igual que en SIATE. */
@Tag("oracle")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OracleFreeDePrueba.class, OracleCapasRepository.class})
class OracleCapasRepositoryOracleTest {

    @Autowired
    private OracleCapasRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararEsquemaDeSiante() {
        if (jdbc.queryForObject("select count(*) from user_tables where table_name = 'LAYERS'", Integer.class) == 0) {
            jdbc.execute("create sequence SEQ_LAYERS start with 500");
            jdbc.execute("create table LAYERS (ID number primary key, OWNER number, NAME varchar2(200), ORIGIN varchar2(20),"
                    + " STATUS varchar2(20), JSON_METADATA clob, DESCRIPTION varchar2(1000), VISIBILITY varchar2(20),"
                    + " L_GROUP_ID number, CATEGORY_ID number, CREATED_DATE date default sysdate, FILE_BASE64 clob)");
            jdbc.execute("create or replace trigger TRG_LAYERS before insert on LAYERS for each row"
                    + " begin :new.ID := SEQ_LAYERS.nextval; end;");
        }
    }

    @Test
    void creaUnaCapaEnBlancoYRecibeElIdDelTrigger() {
        int id = repositorio.crear(7, new NuevaCapa("Bodegas", "Región"));

        assertThat(id).isGreaterThanOrEqualTo(500);
        assertThat(jdbc.queryForMap("select OWNER, NAME, DESCRIPTION, STATUS, VISIBILITY from LAYERS where ID = ?", id).values())
                .extracting(String::valueOf).containsExactly("7", "Bodegas", "Región", "1", "0");
        assertThat(jdbc.queryForObject("select count(*) from LAYERS where ID = ? and JSON_METADATA is null"
                + " and ORIGIN is null and CATEGORY_ID is null and CREATED_DATE is not null", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void unaDescripcionVaciaQuedaComoNullEnOracle() {
        int id = repositorio.crear(7, new NuevaCapa("Bodegas", ""));

        assertThat(jdbc.queryForObject("select count(*) from LAYERS where ID = ? and DESCRIPTION is null", Integer.class, id))
                .isEqualTo(1);
    }
}
