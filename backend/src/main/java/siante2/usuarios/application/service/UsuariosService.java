package siante2.usuarios.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import siante2.usuarios.application.port.in.ObtenerUsuarioActualUseCase;
import siante2.usuarios.application.port.out.UsuariosPort;
import siante2.usuarios.domain.Usuario;

@Service
public class UsuariosService implements ObtenerUsuarioActualUseCase {

    private final UsuariosPort usuarios;

    public UsuariosService(UsuariosPort usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Usuario obtener(String ivUser, List<String> grupos) {
        return usuarios.buscarPorIvUser(ivUser, grupos).orElseGet(() -> {
            usuarios.registrar(ivUser, grupos);
            return usuarios.buscarPorIvUser(ivUser, grupos)
                    .orElseThrow(() -> new IllegalStateException("No se pudo registrar el usuario " + ivUser));
        });
    }
}
