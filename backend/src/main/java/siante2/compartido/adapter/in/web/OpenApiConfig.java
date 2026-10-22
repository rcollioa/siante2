package siante2.compartido.adapter.in.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

    /** Jackson siempre escribe todos los campos, así que en el contrato son obligatorios;
     * sin esto, los tipos generados para el frontend quedan todos opcionales. */
    @Bean
    OpenApiCustomizer camposObligatorios() {
        return api -> {
            if (api.getComponents() == null || api.getComponents().getSchemas() == null) {
                return;
            }
            for (Schema<?> esquema : api.getComponents().getSchemas().values()) {
                if (esquema.getProperties() != null) {
                    esquema.setRequired(new ArrayList<>(esquema.getProperties().keySet()));
                }
            }
        };
    }
}
