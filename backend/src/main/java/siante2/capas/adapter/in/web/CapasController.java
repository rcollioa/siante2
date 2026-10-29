package siante2.capas.adapter.in.web;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;

/** Reemplaza a {@code GET /siante/api/users/:ID/layers} y {@code GET /siante/api/layers/:ID} de SIATE. El usuario
 * sale de WebSEAL y no de la URL; sin {@code @Catalogo}, porque son datos de cada usuario. */
@RestController
@RequestMapping("/capas")
public class CapasController {

    private final ListarMisCapasUseCase listar;
    private final VerCapaUseCase ver;
    private final ListarFavoritasUseCase favoritas;
    private final VerCatalogoUseCase catalogo;

    public CapasController(ListarMisCapasUseCase listar, VerCapaUseCase ver, ListarFavoritasUseCase favoritas,
                           VerCatalogoUseCase catalogo) {
        this.listar = listar;
        this.ver = ver;
        this.favoritas = favoritas;
        this.catalogo = catalogo;
    }

    @GetMapping("/mias")
    public CapasResponse listarMisCapas(Authentication autenticacion) {
        return new CapasResponse(listar.listar(autenticacion.getName()));
    }

    /** Reemplaza a {@code users/:ID/favourites} y {@code users/:ID/favourites/formallayers} de SIATE: las dos listas
     * del menú en una sola respuesta, que el frontend separa por visibilidad. */
    @GetMapping("/favoritas")
    public CapasResponse listarCapasFavoritas(Authentication autenticacion) {
        return new CapasResponse(favoritas.listarFavoritas(autenticacion.getName()));
    }

    /** Las pestañas del catálogo de SIATE ("Todas", "Usuario" y "Capas formales"). */
    @GetMapping("/catalogo")
    public CatalogoResponse verCatalogoDeCapas(@RequestParam SeccionCatalogo seccion, Authentication autenticacion) {
        return new CatalogoResponse(catalogo.verCatalogo(seccion, autenticacion.getName()));
    }

    @GetMapping("/{id}")
    public Capa verCapa(@PathVariable int id, Authentication autenticacion) {
        return ver.ver(id, autenticacion.getName());
    }

    public record CapasResponse(List<ResumenCapa> data) {
    }

    public record CatalogoResponse(List<CapaCatalogo> data) {
    }
}
