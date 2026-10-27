package siante2.usuarios.application.port.in;

import java.util.Optional;

/** ID en {@code USERS} de quien hace la solicitud, para que otros módulos filtren por dueño. A diferencia de
 * {@link ObtenerUsuarioActualUseCase}, no lo registra: sirve para lecturas. */
public interface IdentificarUsuarioUseCase {

    Optional<Integer> idDe(String ivUser);
}
