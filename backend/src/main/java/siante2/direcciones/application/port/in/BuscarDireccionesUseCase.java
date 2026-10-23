package siante2.direcciones.application.port.in;

import java.util.List;
import siante2.direcciones.domain.Direccion;

public interface BuscarDireccionesUseCase {

    /** Direcciones que coinciden con el texto, de la más a la menos relevante (máximo 5). */
    List<Direccion> buscar(String texto);
}
