package siante2.capas.adapter.in.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import siante2.capas.application.port.in.GuardarElementosUseCase;
import siante2.capas.application.port.in.ImportarCapaUseCase;
import siante2.capas.domain.CambiosElementos;
import siante2.capas.domain.CapaImportada;
import siante2.capas.domain.CapaPropia;

/** Los elementos de las capas propias: dibujarlos y traerlos de un archivo. */
@RestController
@RequestMapping("/capas")
public class ElementosController {

    private final GuardarElementosUseCase guardar;
    private final ImportarCapaUseCase importar;

    public ElementosController(GuardarElementosUseCase guardar, ImportarCapaUseCase importar) {
        this.guardar = guardar;
        this.importar = importar;
    }

    /** Reemplaza a {@code POST /siante/api/layers} con un archivo: el navegador lo lee y aquí llegan sus elementos. SIATE
     * tomaba el dueño del cuerpo y guardaba además el archivo y una copia en {@code JSON_METADATA}, que no volvía a usar. */
    @PostMapping("/importar")
    @ResponseStatus(HttpStatus.CREATED)
    public CapaPropia importarCapa(@RequestBody CapaImportada capa, Authentication autenticacion) {
        return importar.importar(autenticacion.getName(), capa);
    }

    /** Reemplaza a {@code POST} y {@code PATCH /siante/api/layersfeatures} y a {@code POST .../:ID/layer_delete/:LAYER_ID}
     * de SIATE, que no verificaban el dueño y aplicaban los cambios de a uno: si fallaba uno, quedaban a medias. */
    @PostMapping("/{id}/elementos")
    public ElementosGuardados guardarElementosDeCapa(@PathVariable int id, @RequestBody CambiosElementos cambios,
                                                     Authentication autenticacion) {
        return new ElementosGuardados(guardar.guardar(id, autenticacion.getName(), cambios));
    }

    /** Los ID que recibieron los elementos nuevos, en el orden en que llegaron. */
    public record ElementosGuardados(List<Integer> nuevos) {
    }
}
