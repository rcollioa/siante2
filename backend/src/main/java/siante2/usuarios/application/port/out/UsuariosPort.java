package siante2.usuarios.application.port.out;

import java.util.List;
import java.util.Optional;
import siante2.usuarios.domain.Usuario;

public interface UsuariosPort {

    Optional<Usuario> buscarPorIvUser(String ivUser, List<String> grupos);

    void registrar(String ivUser, List<String> grupos);
}
