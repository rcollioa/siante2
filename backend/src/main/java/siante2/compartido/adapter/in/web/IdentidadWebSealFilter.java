package siante2.compartido.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Autentica con las cabeceras {@code iv-user}/{@code iv-groups} que pone WebSEAL, como {@code Auth} de SIATE.
 * Ingeniería garantiza que solo WebSEAL llega a la app; {@code usuarioLocal} solo se usa con el perfil local. */
public class IdentidadWebSealFilter extends OncePerRequestFilter {

    public static final String CABECERA_USUARIO = "iv-user";
    public static final String CABECERA_GRUPOS = "iv-groups";
    public static final String CLAVE_MDC = "usuario";
    private static final String SIN_AUTENTICAR = "Unauthenticated";
    /** Un iv-user más largo no viene de WebSEAL y no se registra. Los caracteres de control ya los rechaza el
     * firewall de Spring Security con 400, antes de llegar aquí. */
    private static final int LARGO_MAXIMO_USUARIO = 256;
    private static final Logger log = LoggerFactory.getLogger(IdentidadWebSealFilter.class);

    private final String usuarioLocal;

    public IdentidadWebSealFilter(String usuarioLocal) {
        this.usuarioLocal = usuarioLocal == null || usuarioLocal.isBlank() ? null : usuarioLocal.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabeceraUsuario = request.getHeader(CABECERA_USUARIO);
        if (cabeceraUsuario != null && cabeceraUsuario.length() > LARGO_MAXIMO_USUARIO) {
            log.warn("Cabecera iv-user rechazada por largo ({} caracteres)", cabeceraUsuario.length());
            chain.doFilter(request, response);
            return;
        }
        String usuario = valida(cabeceraUsuario);
        String grupos = grupos(request.getHeader(CABECERA_GRUPOS));
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

    /** Como {@code Auth} de SIATE: la cabecera debe llegar, pero vacía es un usuario sin grupos. */
    private static String grupos(String valor) {
        return valor == null || SIN_AUTENTICAR.equals(valor.trim()) ? null : valor.trim();
    }
}
