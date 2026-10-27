package siante2.capas.application.port.in;

import siante2.capas.domain.Capa;

public interface VerCapaUseCase {

    /** La capa con sus elementos si es del usuario, pública o formal. Lanza {@code RecursoNoEncontradoException}
     * si no existe o es privada de otro usuario, sin distinguir los casos. */
    Capa ver(int capaId, String ivUser);
}
