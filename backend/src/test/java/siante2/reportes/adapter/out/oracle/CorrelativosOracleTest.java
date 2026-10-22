package siante2.reportes.adapter.out.oracle;

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

/** Las vistas NEXTVAL_* leídas por JPQL entregan un correlativo nuevo en cada consulta. */
@Tag("oracle")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleFreeDePrueba.class)
class CorrelativosOracleTest {

    @Autowired
    private CorrelativosJpaRepository correlativos;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararVistasDeSiante() {
        if (jdbc.queryForObject("select count(*) from user_views where view_name = 'NEXTVAL_PUBLIC_REPORTS'", Integer.class) == 0) {
            for (String nombre : new String[] {"PUBLIC_REPORTS", "PUBLIC_REPORTS_DETAILS"}) {
                jdbc.execute("create sequence SEQ_" + nombre + " start with 7");
                jdbc.execute("create or replace function SIG_" + nombre + " return number is begin return SEQ_"
                        + nombre + ".nextval; end;");
                jdbc.execute("create view NEXTVAL_" + nombre + " as select SIG_" + nombre + "() CORRELATIVO from dual");
            }
        }
    }

    @Test
    void cadaConsultaEntregaElSiguienteCorrelativo() {
        int primero = correlativos.siguienteReporte();

        assertThat(correlativos.siguienteReporte()).isEqualTo(primero + 1);
        assertThat(correlativos.siguienteDetalleReporte()).isPositive();
    }
}
