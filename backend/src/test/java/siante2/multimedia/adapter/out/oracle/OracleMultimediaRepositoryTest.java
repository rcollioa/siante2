package siante2.multimedia.adapter.out.oracle;

import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.ContadorSql;
import siante2.compartido.adapter.out.CacheConfig;
import siante2.multimedia.domain.Multimedia;

import static org.assertj.core.api.Assertions.assertThat;

/** MULTIMEDIA en H2 con la caché real: el listado no trae el BLOB y la segunda lectura no va a la base. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ImportAutoConfiguration(CacheAutoConfiguration.class)
@Import({CacheConfig.class, OracleMultimediaRepository.class})
class OracleMultimediaRepositoryTest {

    @Autowired
    private OracleMultimediaRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory fabrica;

    @BeforeEach
    void preparar() {
        jdbc.update("delete from MULTIMEDIA");
        jdbc.update("insert into MULTIMEDIA (X_MULTIMEDIA, DESCRIPCION, ARCHIVO) values (?, ?, ?)",
                1, "Manual SIATE", "%PDF-1.7".getBytes(StandardCharsets.ISO_8859_1));
        jdbc.update("insert into MULTIMEDIA (X_MULTIMEDIA, DESCRIPCION) values (?, ?)", 2, "Gif Metadata");
    }

    @Test
    void listaSinArchivosYLaSegundaVezSaleDeLaCache() {
        ContadorSql sql = new ContadorSql(fabrica);

        assertThat(repositorio.listar()).containsExactly(new Multimedia(2, "Gif Metadata"), new Multimedia(1, "Manual SIATE"));
        assertThat(repositorio.listar()).hasSize(2);
        assertThat(sql.sentencias()).isEqualTo(1);
    }

    @Test
    void leeElContenidoDeUnArchivo() {
        var contenido = repositorio.contenido(1).orElseThrow();

        assertThat(contenido.descripcion()).isEqualTo("Manual SIATE");
        assertThat(new String(contenido.bytes(), StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-1.7");
        assertThat(repositorio.contenido(99)).isEmpty();
    }
}
