package siante2.usuarios.application.port.in;

import java.util.List;
import siante2.usuarios.domain.Usuario;

public interface ObtenerUsuarioActualUseCase {

    /** Usuario autenticado; lo registra en {@code USERS} la primera vez, como {@code Auth} de SIANTE. */
    Usuario obtener(String ivUser, List<String> grupos);
}
