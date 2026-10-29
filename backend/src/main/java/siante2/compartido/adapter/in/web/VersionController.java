package siante2.compartido.adapter.in.web;

import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Versión y fecha de compilación del backend (build-info de Maven), para mostrarlas en el menú: la mesa de ayuda
 * sabe qué versión tiene el usuario, como la página /version de SIATE. Actuator también la tiene, pero en el puerto
 * de administración, que no se publica. Sin caso de uso: es información de la compilación, no del negocio. */
@RestController
public class VersionController {

    private final Version version;

    public VersionController(ObjectProvider<BuildProperties> compilacion) {
        BuildProperties propiedades = compilacion.getIfAvailable();
        this.version = propiedades == null
                ? new Version("desconocida", null)
                : new Version(propiedades.getVersion(), propiedades.getTime());
    }

    @GetMapping("/version")
    public Version obtenerVersion() {
        return version;
    }

    /** @param compilada cuándo se compiló; null si la compilación no dejó build-info. */
    public record Version(String version, Instant compilada) {
    }
}
