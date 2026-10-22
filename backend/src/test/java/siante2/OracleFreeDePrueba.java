package siante2;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.oracle.OracleContainer;

/** Oracle Free en Docker como datasource de los tests @Tag("oracle") (mvnw test -Poracle). Trae Spatial, no Java. */
@TestConfiguration(proxyBeanMethods = false)
public class OracleFreeDePrueba {

    @Bean
    @ServiceConnection
    OracleContainer oracleFree() {
        return new OracleContainer("gvenzl/oracle-free:23-faststart");
    }
}
