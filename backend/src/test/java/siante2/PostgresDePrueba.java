package siante2;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL en Docker como datasource de los tests @Tag("postgres") (mvnw test -Ppostgres): prueba de concepto del
 * paso de Oracle a PostgreSQL (docs/modelo-datos.md). Flyway carga el esquema espejo de Oracle. Como el servidor de la
 * CGR: PostgreSQL 16 sin PostGIS. */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresDePrueba {

    /** Activa Flyway (apagado en el perfil test) con las migraciones de src/test/resources/db/postgresql. */
    public static final String FLYWAY = "spring.flyway.enabled=true";
    public static final String MIGRACIONES = "spring.flyway.locations=classpath:db/postgresql";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
    }
}
