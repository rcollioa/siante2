package siante2.capas.application.port.in;

import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.NuevaCapa;

public interface CrearCapaUseCase {
    /** La capa queda activa, privada y sin elementos. Lanza {@code SolicitudInvalidaException} si los datos no son
     * válidos y {@code RecursoNoEncontradoException} si el usuario no está registrado. */
    CapaPropia crear(String ivUser, NuevaCapa nueva);
}
