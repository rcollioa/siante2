package siante2.adapter.in.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Stream;
import siante2.domain.SolicitudInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones de la API a respuestas JSON con un código estable ({@link ApiErrorResponse}),
 * para que el frontend no dependa del mensaje ni del cuerpo de error por defecto de Spring.
 *
 * <p>Toda respuesta de error tiene el mismo formato, también las que arma Spring MVC (validación,
 * parámetro faltante o mal tipado, JSON mal formado, 404, 405...): hereda de
 * {@link ResponseEntityExceptionHandler}, que ya conoce el estado HTTP de cada una, y solo cambia el
 * cuerpo en {@link #handleExceptionInternal}. Los mensajes son para el usuario: nunca llevan el texto
 * interno de la excepción, salvo los de validación (escritos a propósito en las anotaciones) y
 * {@link SolicitudInvalidaException}.
 *
 * <p>Si se agrega Spring Security, sus excepciones ({@code AccessDeniedException},
 * {@code AuthenticationException}) necesitan su propio handler aquí: si no, las toma
 * {@link #handleInesperada} y responden 500.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private static final String MENSAJE_INESPERADO = "Ocurrió un error inesperado.";

    /** Validación de entrada hecha a propósito por los casos de uso: una línea en el log, sin
     * stacktrace, porque el error es del cliente y el stack no agrega nada. */
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> handleSolicitudInvalida(SolicitudInvalidaException exception) {
        log.warn("Solicitud inválida: {}", exception.getMessage());
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        // Cualquier otra IllegalArgumentException no la lanzó un caso de uso a propósito (p. ej. una
        // librería de terceros): se responde igual 400, pero con el stacktrace en el log, porque puede
        // ser un bug real y el cliente solo ve el mensaje.
        log.warn("Solicitud inválida no prevista: {}", exception.getMessage(), exception);
        return badRequest(exception.getMessage());
    }

    /** Bean Validation fuera de los controladores (p. ej. un servicio anotado con {@code @Validated}):
     * Spring MVC no la conoce, pero sigue siendo una entrada rechazada, no una falla del servidor. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        String mensaje = unir(exception.getConstraintViolations().stream()
                .map(ApiExceptionHandler::describir));
        log.warn("Solicitud inválida: {}", mensaje);
        return badRequest(mensaje);
    }

    private static ResponseEntity<ApiErrorResponse> badRequest(String mensaje) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(codigo(HttpStatus.BAD_REQUEST), mensaje));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccess(DataAccessException exception) {
        log.error("Data access error while serving a request", exception);
        return fuenteNoDisponible();
    }

    /**
     * {@code CannotCreateTransactionException} (p. ej. el pool de Hikari no logra abrir una conexión
     * porque la base no responde) es un {@code TransactionException}, no un {@code DataAccessException}:
     * la falla ocurre al abrir la transacción, antes de que Hibernate llegue a traducir un
     * {@code SQLException}. Sin este handler, la misma caída de base daría 503 en unas peticiones y 500
     * sin cuerpo JSON en otras, según en qué punto exacto se corte la conexión.
     */
    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ApiErrorResponse> handleTransaction(TransactionException exception) {
        log.error("Transaction error while serving a request", exception);
        return fuenteNoDisponible();
    }

    private static ResponseEntity<ApiErrorResponse> fuenteNoDisponible() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("DATA_SOURCE_UNAVAILABLE",
                        "No fue posible consultar la información solicitada."));
    }

    /**
     * Cualquier otra excepción (un bug): 500 con mensaje genérico y el stacktrace en el log. Sin esto,
     * Spring responde con su propio cuerpo de error. Las excepciones con {@code @ResponseStatus}
     * conservan su estado, igual que sin este handler.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleInesperada(Exception exception) {
        ResponseStatus anotada = AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
        HttpStatus estado = anotada != null ? anotada.code() : HttpStatus.INTERNAL_SERVER_ERROR;
        registrar(exception, estado);
        String mensaje = anotada != null && !anotada.reason().isEmpty() ? anotada.reason() : mensajePorEstado(estado);
        return ResponseEntity.status(estado).body(new ApiErrorResponse(codigo(estado), mensaje));
    }

    /** Punto único por el que pasan las excepciones que resuelve {@link ResponseEntityExceptionHandler}:
     * conserva su estado y sus cabeceras (p. ej. {@code Allow} en un 405) y reemplaza el cuerpo. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        registrar(exception, statusCode);
        ApiErrorResponse error = new ApiErrorResponse(codigo(statusCode), mensaje(exception, statusCode));
        return super.handleExceptionInternal(exception, error, headers, statusCode, request);
    }

    /** Errores del cliente en una línea; del servidor, con stacktrace. */
    private static void registrar(Exception exception, HttpStatusCode estado) {
        if (estado.is5xxServerError()) {
            log.error("Unexpected error while serving a request", exception);
        } else {
            log.warn("Solicitud rechazada ({}): {}", estado.value(), exception.getMessage());
        }
    }

    /** Se mantiene {@code INVALID_REQUEST} para el 400, que ya usa el frontend; el resto, el nombre del
     * estado HTTP ({@code NOT_FOUND}, {@code METHOD_NOT_ALLOWED}...). */
    private static String codigo(HttpStatusCode estado) {
        HttpStatus conocido = HttpStatus.resolve(estado.value());
        if (conocido == HttpStatus.BAD_REQUEST) {
            return "INVALID_REQUEST";
        }
        return conocido != null ? conocido.name() : "ERROR";
    }

    private static String mensaje(Exception exception, HttpStatusCode estado) {
        return switch (exception) {
            case MethodArgumentNotValidException e -> unir(Stream.concat(
                    e.getBindingResult().getFieldErrors().stream().map(ApiExceptionHandler::describir),
                    e.getBindingResult().getGlobalErrors().stream().map(MessageSourceResolvable::getDefaultMessage)));
            // Antes que ResponseStatusException, de la que hereda.
            case HandlerMethodValidationException e -> unir(e.getParameterValidationResults().stream()
                    .flatMap(resultado -> resultado.getResolvableErrors().stream()
                            .map(error -> resultado.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage())));
            case MissingServletRequestParameterException e -> "Falta el parámetro '" + e.getParameterName() + "'.";
            case TypeMismatchException e -> "El parámetro '" + e.getPropertyName() + "' no tiene un formato válido.";
            case HttpMessageNotReadableException e -> "El cuerpo de la solicitud no es válido.";
            // Lanzada a propósito por el código del proyecto, con un motivo pensado para el cliente.
            case ResponseStatusException e when e.getReason() != null && estado.is4xxClientError() -> e.getReason();
            default -> mensajePorEstado(estado);
        };
    }

    private static String mensajePorEstado(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 400 -> "La solicitud no es válida.";
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "El método HTTP no está permitido para este recurso.";
            case 406 -> "No hay una representación disponible en el formato pedido.";
            case 415 -> "El tipo de contenido de la solicitud no está soportado.";
            default -> estado.is4xxClientError() ? "La solicitud no es válida." : MENSAJE_INESPERADO;
        };
    }

    private static String describir(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private static String describir(ConstraintViolation<?> violacion) {
        return violacion.getPropertyPath() + ": " + violacion.getMessage();
    }

    private static String unir(Stream<String> mensajes) {
        List<String> lista = mensajes.sorted().toList();
        return lista.isEmpty() ? "La solicitud no es válida." : String.join("; ", lista);
    }

    public record ApiErrorResponse(String code, String message) {
    }
}
