package siante2.capas.application.port.in;

import java.util.List;
import siante2.capas.domain.ResumenCapa;

public interface ListarMisCapasUseCase {

    /** Capas activas del usuario identificado por WebSEAL, por nombre en orden alfabético español. */
    List<ResumenCapa> listar(String ivUser);
}
