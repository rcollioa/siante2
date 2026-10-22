package siante2.compartido.domain;

/** Entrada rechazada a propósito por un caso de uso: 400 y log en una línea, sin stacktrace.
 * Otras IllegalArgumentException se tratan como inesperadas. */
public class SolicitudInvalidaException extends IllegalArgumentException {

    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
