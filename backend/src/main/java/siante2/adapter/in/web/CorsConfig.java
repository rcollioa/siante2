package siante2.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Orígenes permitidos ({@code APP_CORS_ALLOWED_ORIGINS}, separados por coma); sin lista no hay regla.
 * El frontend no lo necesita (mismo dominio) y no se aceptan credenciales. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String prefijo;
    private final String[] origenesPermitidos;

    public CorsConfig(@Value("${app.api.prefijo:/siante2/api}") String prefijo,
                      @Value("${app.cors.allowed-origins:}") String[] origenesPermitidos) {
        this.prefijo = prefijo;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origenesPermitidos.length == 0) {
            return;
        }
        registry.addMapping(prefijo + "/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
