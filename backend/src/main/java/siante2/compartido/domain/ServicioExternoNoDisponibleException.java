package siante2.compartido.domain;

/** Un servicio fuera de SIANTE (p. ej. geocodificación) no respondió o respondió con error: 503. */
public class ServicioExternoNoDisponibleException extends RuntimeException {

    public ServicioExternoNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
