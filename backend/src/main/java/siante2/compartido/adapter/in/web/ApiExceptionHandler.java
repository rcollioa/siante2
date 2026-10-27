package siante2.compartido.adapter.in.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Stream;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.compartido.domain.SolicitudInvalidaException;
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
import org.springframework.security.access.AccessDeniedException;
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

/** Todo error de la API responde {@link ApiErrorResponse}, también los que arma Spring MVC. Las excepciones de
 * Spring Security que llegan al controlador (p. ej. de {@code @PreAuthorize}) necesitan handler aquí o responden 500. */
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

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleNoEncontrado(RecursoNoEncontradoException exception) {
        log.info("Recurso no encontrado: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(codigo(HttpStatus.NOT_FOUND), exception.getMessage()));
    }

    @ExceptionHandler(ServicioExternoNoDisponibleException.class)
    public ResponseEntity<ApiErrorResponse> handleServicioExterno(ServicioExternoNoDisponibleException exception) {
        log.warn("{} Causa: {}", exception.getMessage(), String.valueOf(exception.getCause()));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("EXTERNAL_SERVICE_UNAVAILABLE", exception.getMessage()));
    }

    /** {@code @PreAuthorize} rechaza dentro del controlador, donde ya no actúa el accessDeniedHandler de
     * SeguridadConfig; mismo 403 y mensaje que ese handler. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccesoDenegado(AccessDeniedException exception) {
        log.warn("Acceso denegado: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(codigo(HttpStatus.FORBIDDEN), "No tiene permiso para esta operación."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        // IllegalArgumentException no lanzada por un caso de uso (p. ej. una librería): 400 igual,
        // pero con stacktrace en el log porque puede ser un bug.
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

    /** Una base caída al abrir la transacción lanza TransactionException, no DataAccessException:
     * sin este handler respondería 500 en vez de 503. */
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

    /** Cualquier otra excepción es un bug: 500 genérico y stacktrace en el log.
     * Las excepciones con {@code @ResponseStatus} conservan su estado. */
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
