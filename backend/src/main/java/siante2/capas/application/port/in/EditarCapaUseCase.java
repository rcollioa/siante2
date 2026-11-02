package siante2.capas.application.port.in;

import siante2.capas.domain.EdicionCapa;

public interface EditarCapaUseCase {
    /** Lanza {@code SolicitudInvalidaException} si los datos no son válidos y {@code RecursoNoEncontradoException} si
     * la capa no existe o no es del usuario, sin distinguir los casos. */
    void editar(int capaId, String ivUser, EdicionCapa edicion);
}
