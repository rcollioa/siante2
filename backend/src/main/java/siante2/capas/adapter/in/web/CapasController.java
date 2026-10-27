package siante2.capas.adapter.in.web;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.domain.Capa;
import siante2.capas.domain.ResumenCapa;

/** Reemplaza a {@code GET /siante/api/users/:ID/layers} y {@code GET /siante/api/layers/:ID} de SIATE. El usuario
 * sale de WebSEAL y no de la URL; sin {@code @Catalogo}, porque son datos de cada usuario. */
@RestController
@RequestMapping("/capas")
public class CapasController {

    private final ListarMisCapasUseCase listar;
    private final VerCapaUseCase ver;

    public CapasController(ListarMisCapasUseCase listar, VerCapaUseCase ver) {
        this.listar = listar;
        this.ver = ver;
    }

    @GetMapping("/mias")
    public CapasResponse listarMisCapas(Authentication autenticacion) {
        return new CapasResponse(listar.listar(autenticacion.getName()));
    }

    @GetMapping("/{id}")
    public Capa verCapa(@PathVariable int id, Authentication autenticacion) {
        return ver.ver(id, autenticacion.getName());
    }

    public record CapasResponse(List<ResumenCapa> data) {
    }
}
