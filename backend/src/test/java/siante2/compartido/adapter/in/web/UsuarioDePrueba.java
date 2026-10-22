package siante2.compartido.adapter.in.web;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Para {@code @WebMvcTest}: seguridad real y cada petición con usuario de WebSEAL y token CSRF. */
@TestConfiguration
@Import(SeguridadConfig.class)
public class UsuarioDePrueba {

    @Bean
    MockMvcBuilderCustomizer usuarioEnCadaPeticion() {
        return builder -> builder.defaultRequest(get("/")
                .header(IdentidadWebSealFilter.CABECERA_USUARIO, "prueba")
                .header(IdentidadWebSealFilter.CABECERA_GRUPOS, "PRUEBA")
                .with(csrf()));
    }
}
