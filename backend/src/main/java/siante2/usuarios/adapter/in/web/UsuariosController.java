package siante2.usuarios.adapter.in.web;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import siante2.usuarios.application.port.in.ObtenerUsuarioActualUseCase;
import siante2.usuarios.domain.Usuario;

/** Reemplaza a {@code GET /siante/api/auth} de SIATE. */
@RestController
@RequestMapping("/usuarios")
public class UsuariosController {

    private static final String PREFIJO_GRUPO = "GRUPO_";

    private final ObtenerUsuarioActualUseCase usuarioActual;

    public UsuariosController(ObtenerUsuarioActualUseCase usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    @GetMapping("/actual")
    public Usuario obtenerUsuarioActual(Authentication autenticacion) {
        List<String> grupos = autenticacion.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith(PREFIJO_GRUPO))
                .map(a -> a.substring(PREFIJO_GRUPO.length()))
                .toList();
        return usuarioActual.obtener(autenticacion.getName(), grupos);
    }
}
