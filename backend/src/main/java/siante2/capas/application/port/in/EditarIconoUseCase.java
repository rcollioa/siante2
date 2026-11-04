package siante2.capas.application.port.in;

import siante2.capas.domain.DatosIcono;

public interface EditarIconoUseCase {
    /** Los elementos que ya lo usan conservan su copia. Lanza {@code SolicitudInvalidaException} si los datos no son válidos
     * y {@code RecursoNoEncontradoException} si no existe o no es del usuario. */
    void editar(int id, String ivUser, DatosIcono datos);
}
