package siante2.compartido.domain;

/** El recurso pedido no existe (o el usuario no puede verlo): 404 con el mensaje, sin stacktrace. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
