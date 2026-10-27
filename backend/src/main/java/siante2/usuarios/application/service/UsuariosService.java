package siante2.usuarios.application.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;
import siante2.usuarios.application.port.in.ObtenerUsuarioActualUseCase;
import siante2.usuarios.application.port.out.UsuariosPort;
import siante2.usuarios.domain.Usuario;

@Service
public class UsuariosService implements ObtenerUsuarioActualUseCase, IdentificarUsuarioUseCase {

    private final UsuariosPort usuarios;

    public UsuariosService(UsuariosPort usuarios) {
        this.usuarios = usuarios;
    }

    /** Buscar, registrar y releer en una sola transacción. */
    @Override
    @Transactional
    public Usuario obtener(String ivUser, List<String> grupos) {
        return usuarios.buscarPorIvUser(ivUser, grupos).orElseGet(() -> {
            usuarios.registrar(ivUser, grupos);
            return usuarios.buscarPorIvUser(ivUser, grupos)
                    .orElseThrow(() -> new IllegalStateException("No se pudo registrar el usuario " + ivUser));
        });
    }

    @Override
    public Optional<Integer> idDe(String ivUser) {
        return usuarios.buscarPorIvUser(ivUser, List.of()).map(Usuario::id);
    }
}
