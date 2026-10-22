package siante2.categorias.adapter.out.oracle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.categorias.domain.Categoria;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapeo de {@code CATEGORIES} contra H2, con la tabla creada desde la entidad. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class OracleCategoriasRepositoryTest {

    @Autowired
    private CategoriaJpaRepository jpa;

    @Autowired
    private JdbcTemplate jdbc;

    private OracleCategoriasRepository repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new OracleCategoriasRepository(jpa);
        String insertar = "insert into CATEGORIES (ID, NAME, DESCRIPTION, STATUS) values (?, ?, ?, ?)";
        jdbc.update(insertar, 1, "Salud", "Centros de salud", "1");
        jdbc.update(insertar, 2, "Antigua", "Ya no se usa", "0");
        jdbc.update(insertar, 3, "Educación", null, "1");
    }

    @Test
    void devuelveSoloLasActivasOrdenadasPorNombreSinDescripcionesNulas() {
        assertThat(repositorio.buscarActivas()).containsExactly(
                new Categoria(3, "Educación", ""),
                new Categoria(1, "Salud", "Centros de salud"));
    }
}
