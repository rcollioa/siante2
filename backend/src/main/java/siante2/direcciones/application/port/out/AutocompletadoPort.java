package siante2.direcciones.application.port.out;

import java.util.List;
import java.util.Optional;
import siante2.direcciones.domain.Direccion;
import siante2.direcciones.domain.ZonaVisible;

/** Sugerencias mientras se escribe: un servicio pensado para autocompletar (Nominatim lo prohíbe). */
public interface AutocompletadoPort {

    /** La zona ordena por cercanía sin excluir el resto. Lanza {@code ServicioExternoNoDisponibleException}
     * si el servicio no responde. */
    List<Direccion> sugerir(String texto, Optional<ZonaVisible> zona);
}
