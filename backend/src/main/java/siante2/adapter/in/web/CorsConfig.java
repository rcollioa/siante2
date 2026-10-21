package siante2.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Orígenes que pueden llamar a la API desde otro dominio ({@code app.cors.allowed-origins}, que en k8s
 * viene de {@code APP_CORS_ALLOWED_ORIGINS}, separados por coma). El frontend no lo necesita: el ingress
 * lo publica en el mismo dominio que la API. Sin orígenes configurados no se agrega ninguna regla y el
 * navegador bloquea toda llamada desde otro dominio.
 *
 * <p>Solo se aceptan los orígenes de la lista, sin cookies ni cabeceras de autenticación. Si el proyecto
 * necesita credenciales u otros métodos, ajustar {@code allowCredentials}/{@code allowedMethods}.
 */
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
