package siante2.capas.application.port.in;

import siante2.capas.domain.CapaImportada;
import siante2.capas.domain.CapaPropia;

public interface ImportarCapaUseCase {
    /** Crea una capa privada con sus elementos, todo o nada. Lanza {@code SolicitudInvalidaException} si los datos no son
     * válidos y {@code RecursoNoEncontradoException} si el usuario no está registrado. */
    CapaPropia importar(String ivUser, CapaImportada capa);
}
