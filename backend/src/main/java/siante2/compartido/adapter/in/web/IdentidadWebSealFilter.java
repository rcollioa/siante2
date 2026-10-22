package siante2.compartido.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Autentica con las cabeceras {@code iv-user}/{@code iv-groups} que pone WebSEAL, como {@code Auth} de SIANTE.
 * El ingress debe eliminar las que manda el cliente; {@code usuarioLocal} solo se usa con el perfil local. */
public class IdentidadWebSealFilter extends OncePerRequestFilter {

    public static final String CABECERA_USUARIO = "iv-user";
    public static final String CABECERA_GRUPOS = "iv-groups";
    public static final String CLAVE_MDC = "usuario";
    private static final String SIN_AUTENTICAR = "Unauthenticated";

    private final String usuarioLocal;

    public IdentidadWebSealFilter(String usuarioLocal) {
        this.usuarioLocal = usuarioLocal == null || usuarioLocal.isBlank() ? null : usuarioLocal.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String usuario = valida(request.getHeader(CABECERA_USUARIO));
        String grupos = valida(request.getHeader(CABECERA_GRUPOS));
        if (usuario == null && usuarioLocal != null) {
            usuario = usuarioLocal;
            grupos = "";
        }
        if (usuario == null || grupos == null) {
            chain.doFilter(request, response);
            return;
        }
        List<SimpleGrantedAuthority> autoridades = Arrays.stream(grupos.split(","))
                .map(String::trim).filter(g -> !g.isEmpty())
                .map(g -> new SimpleGrantedAuthority("GRUPO_" + g))
                .toList();
        var autenticacion = new PreAuthenticatedAuthenticationToken(usuario, grupos, autoridades);
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
        MDC.put(CLAVE_MDC, usuario);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_MDC);
            SecurityContextHolder.clearContext();
        }
    }

    private static String valida(String valor) {
        return valor == null || valor.isBlank() || SIN_AUTENTICAR.equals(valor.trim()) ? null : valor.trim();
    }
}
