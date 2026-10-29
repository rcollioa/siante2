package siante2.capas.application.port.in;

import java.util.List;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.SeccionCatalogo;

public interface VerCatalogoUseCase {

    /** Las capas de una pestaña del catálogo, por nombre, marcando cuáles son favoritas del usuario. */
    List<CapaCatalogo> verCatalogo(SeccionCatalogo seccion, String ivUser);
}
