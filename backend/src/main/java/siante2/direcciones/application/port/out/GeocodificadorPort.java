package siante2.direcciones.application.port.out;

import java.util.List;
import siante2.direcciones.domain.Direccion;

public interface GeocodificadorPort {

    /** Lanza {@code ServicioExternoNoDisponibleException} si el servicio no responde. */
    List<Direccion> buscar(String texto);
}
