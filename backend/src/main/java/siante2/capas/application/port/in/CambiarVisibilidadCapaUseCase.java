package siante2.capas.application.port.in;

import siante2.capas.domain.Visibilidad;

public interface CambiarVisibilidadCapaUseCase {
    /** Publica o despublica. Lanza {@code SolicitudInvalidaException} si no es privada ni pública y
     * {@code RecursoNoEncontradoException} si la capa no existe o no es del usuario, sin distinguir los casos. */
    void cambiarVisibilidad(int capaId, String ivUser, Visibilidad visibilidad);
}
