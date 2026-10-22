package siante2.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Agrega {@code app.api.prefijo} a todos los {@code @RestController}; Actuator conserva su ruta.
 * Si cambia, ajustar API_PREFIJO del frontend, los proxy.conf*.json y los ingress. */
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
