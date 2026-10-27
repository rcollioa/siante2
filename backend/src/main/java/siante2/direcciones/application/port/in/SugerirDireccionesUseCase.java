package siante2.direcciones.application.port.in;

import java.util.List;
import java.util.Optional;
import siante2.direcciones.domain.Direccion;
import siante2.direcciones.domain.ZonaVisible;

public interface SugerirDireccionesUseCase {

    /** Direcciones que empiezan como el texto que se está escribiendo, de la más a la menos relevante (máximo 5);
     * con la zona del mapa, las cercanas primero. */
    List<Direccion> sugerir(String texto, Optional<ZonaVisible> zona);
}
