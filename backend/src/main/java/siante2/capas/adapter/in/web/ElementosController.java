package siante2.capas.adapter.in.web;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import siante2.capas.application.port.in.GuardarElementosUseCase;
import siante2.capas.domain.CambiosElementos;

/** Reemplaza a {@code POST} y {@code PATCH /siante/api/layersfeatures} y a {@code POST .../:ID/layer_delete/:LAYER_ID}
 * de SIATE, que no verificaban el dueño y aplicaban los cambios de a uno: si fallaba uno, quedaban a medias. */
@RestController
@RequestMapping("/capas/{id}/elementos")
public class ElementosController {

    private final GuardarElementosUseCase guardar;

    public ElementosController(GuardarElementosUseCase guardar) {
        this.guardar = guardar;
    }

    @PostMapping
    public ElementosGuardados guardarElementosDeCapa(@PathVariable int id, @RequestBody CambiosElementos cambios,
                                                     Authentication autenticacion) {
        return new ElementosGuardados(guardar.guardar(id, autenticacion.getName(), cambios));
    }

    /** Los ID que recibieron los elementos nuevos, en el orden en que llegaron. */
    public record ElementosGuardados(List<Integer> nuevos) {
    }
}
