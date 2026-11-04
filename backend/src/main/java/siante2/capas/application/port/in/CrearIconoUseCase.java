package siante2.capas.application.port.in;

import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;

public interface CrearIconoUseCase {
    /** Lanza {@code SolicitudInvalidaException} si los datos no son válidos y {@code RecursoNoEncontradoException} si el
     * usuario no está registrado. */
    Icono crear(String ivUser, DatosIcono datos);
}
