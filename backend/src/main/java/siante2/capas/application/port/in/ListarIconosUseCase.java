package siante2.capas.application.port.in;

import java.util.List;
import siante2.capas.domain.Icono;

public interface ListarIconosUseCase {
    /** Los del sistema y los del usuario; sin registro en USERS, solo los del sistema. */
    List<Icono> listar(String ivUser);
}
