package siante2.capas.application.port.in;

import java.util.List;
import siante2.capas.domain.ResumenCapa;

public interface ListarFavoritasUseCase {

    /** Capas favoritas del usuario identificado por WebSEAL que todavía puede ver, por nombre. El menú separa las
     * formales ("Capas formales") del resto ("Favoritos") por su visibilidad. */
    List<ResumenCapa> listarFavoritas(String ivUser);
}
