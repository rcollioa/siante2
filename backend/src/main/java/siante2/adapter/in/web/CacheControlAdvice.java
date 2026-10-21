package siante2.adapter.in.web;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Agrega {@code Cache-Control} a las respuestas GET exitosas de la API, para que el navegador las
 * reutilice sin volver a pedirlas al backend. Va como {@link ResponseBodyAdvice} y no como interceptor a
 * propósito: solo se ejecuta cuando hay un cuerpo que escribir con estado 2xx, así que un 400 o un 503
 * (ver {@link ApiExceptionHandler}) nunca quedan cacheados por accidente.
 *
 * <p>Aplica a todo {@code @RestController} con la misma duración ({@code app.cache.max-age}). Si un
 * recurso necesita otra política (p. ej. cartografía que casi no cambia), se agrega aquí eligiéndola
 * por el controlador que responde, no por la ruta, para no depender del prefijo de la API (ver
 * {@link ApiPrefijoConfig}). Con {@code PT0S} se desactiva el caché sin tocar el código.
 */
@RestControllerAdvice
public class CacheControlAdvice implements ResponseBodyAdvice<Object> {

    private final CacheControl politica;

    public CacheControlAdvice(@Value("${app.cache.max-age:PT5M}") Duration maxAge) {
        this.politica = politica(maxAge);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getContainingClass().isAnnotationPresent(RestController.class);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (HttpMethod.GET.equals(request.getMethod()) && esExitosa(response)) {
            response.getHeaders().setCacheControl(politica);
        }
        return body;
    }

    private static boolean esExitosa(ServerHttpResponse response) {
        return !(response instanceof ServletServerHttpResponse servlet) || servlet.getServletResponse().getStatus() < 300;
    }

    /** Duración cero o negativa desactiva el caché: el navegador debe revalidar siempre. */
    private static CacheControl politica(Duration maxAge) {
        return maxAge.isPositive() ? CacheControl.maxAge(maxAge).cachePublic() : CacheControl.noCache();
    }
}
