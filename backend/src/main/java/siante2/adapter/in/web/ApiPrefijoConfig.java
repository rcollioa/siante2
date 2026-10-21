package siante2.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Prefijo común de la API ({@code app.api.prefijo}, por defecto {@code /siante2/api}). Se agrega aquí
 * a todos los {@code @RestController}, que declaran solo su recurso ({@code /ejemplo}...). Así el prefijo
 * se cambia en un solo lugar y no queda un {@code /api} genérico que choque con otra aplicación si el
 * sitio se publica bajo un dominio compartido. Actuator ({@code /actuator/health}, usado por los probes
 * de k8s) no es un {@code @RestController} y conserva su ruta.
 *
 * <p>Si se cambia, ajustar también {@code API_PREFIJO} del frontend, los {@code proxy.conf*.json}
 * y el {@code path} de los ingress de k8s.
 */
@Configuration
public class ApiPrefijoConfig implements WebMvcConfigurer {

    private final String prefijo;

    public ApiPrefijoConfig(@Value("${app.api.prefijo:/siante2/api}") String prefijo) {
        this.prefijo = prefijo;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(prefijo, HandlerTypePredicate.forAnnotation(RestController.class));
    }
}
