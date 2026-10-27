package siante2.compartido.adapter.in.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/** Toda la API exige usuario (cabeceras de WebSEAL); salud y contrato quedan abiertos. Sin sesión
 * propia: la identidad llega en cada solicitud. CSRF por cookie, que Angular envía solo. */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, Environment ambiente,
                                  @Value("${app.api.prefijo}") String prefijo,
                                  @Value("${app.seguridad.usuario-local:}") String usuarioLocal) throws Exception {
        String soloLocal = ambiente.acceptsProfiles(Profiles.of("local")) ? usuarioLocal : null;
        http
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.spa())
                .addFilterBefore(new IdentidadWebSealFilter(soloLocal), AbstractPreAuthenticatedProcessingFilter.class)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/error", prefijo + "/openapi").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> error(res, HttpStatus.UNAUTHORIZED,
                                "Se requiere iniciar sesión."))
                        .accessDeniedHandler((req, res, ex) -> error(res, HttpStatus.FORBIDDEN,
                                "No tiene permiso para esta operación.")));
        return http.build();
    }

    /** Mismo formato {@code {code, message}} que ApiExceptionHandler; los mensajes son constantes. */
    private static void error(HttpServletResponse response, HttpStatus estado, String mensaje) throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"" + estado.name() + "\",\"message\":\"" + mensaje + "\"}");
    }
}
