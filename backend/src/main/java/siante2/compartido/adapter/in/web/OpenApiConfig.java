package siante2.compartido.adapter.in.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Datos generales del contrato OpenAPI. El servidor es relativo para que el contrato
 * no dependa del host que lo generó. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI contratoApi() {
        return new OpenAPI()
                .info(new Info().title("SIANTE API").version("1"))
                .servers(List.of(new Server().url("/")));
    }
}
