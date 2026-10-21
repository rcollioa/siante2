package siante2.domain;

/**
 * Parámetro de entrada rechazado a propósito por un caso de uso (año fuera de rango, código mal
 * formado...). {@code ApiExceptionHandler} la responde con 400 y la registra en una sola línea, sin
 * stacktrace: es un error del cliente, no del servidor.
 *
 * <p>Hereda de {@link IllegalArgumentException}; cualquier otra {@code IllegalArgumentException} (p. ej.
 * de una librería) se sigue tratando como inesperada y se registra con su stacktrace.
 */
public class SolicitudInvalidaException extends IllegalArgumentException {

    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
