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

/** Cache-Control ({@code app.cache.max-age}) solo en GET 2xx con cuerpo: un error nunca queda en caché.
 * {@code PT0S} lo desactiva; una política distinta se elige por controlador, no por ruta. */
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
