package siante2.compartido.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/** ETag en las respuestas de la API: con {@code no-cache}, el navegador revalida y, si nada cambió, recibe
 * 304 sin cuerpo. Un endpoint de archivos lo evita con {@code disableContentCaching} (no copia el BLOB). */
@Component
public class EtagApiFilter extends ShallowEtagHeaderFilter {

    private final String prefijo;

    public EtagApiFilter(@Value("${app.api.prefijo}") String prefijo) {
        this.prefijo = prefijo;
    }

    /** Actuator y el resto quedan fuera: los probes de k8s no necesitan ETag. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + prefijo + "/");
    }
}
