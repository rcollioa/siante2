package siante2.capas.application.port.in;

import java.util.List;
import siante2.capas.domain.CambiosElementos;

public interface GuardarElementosUseCase {
    /** Aplica todos los cambios o ninguno y devuelve los ID de los nuevos, en el mismo orden. Lanza
     * {@code SolicitudInvalidaException} si los datos no son válidos y {@code RecursoNoEncontradoException} si la capa no
     * existe, no es del usuario o no se puede dibujar en ella, sin distinguir los casos. */
    List<Integer> guardar(int capaId, String ivUser, CambiosElementos cambios);
}
