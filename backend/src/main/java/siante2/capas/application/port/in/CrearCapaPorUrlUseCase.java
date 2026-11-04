package siante2.capas.application.port.in;

import siante2.capas.domain.CapaPorUrl;
import siante2.capas.domain.CapaPropia;

public interface CrearCapaPorUrlUseCase {
    /** La capa queda activa y privada, y no se pregunta al servidor: si no responde, se abre con el aviso de siempre. Lanza
     * {@code SolicitudInvalidaException} si los datos no son válidos o el mapa no puede mostrar ese tipo de servidor, y
     * {@code RecursoNoEncontradoException} si el usuario no está registrado. */
    CapaPropia crearPorUrl(String ivUser, CapaPorUrl capa);
}
