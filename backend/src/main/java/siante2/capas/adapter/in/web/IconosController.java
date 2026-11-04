package siante2.capas.adapter.in.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import siante2.capas.application.port.in.CrearIconoUseCase;
import siante2.capas.application.port.in.EditarIconoUseCase;
import siante2.capas.application.port.in.EliminarIconoUseCase;
import siante2.capas.application.port.in.ListarIconosUseCase;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;

/** Reemplaza a {@code /siante/api/points_catalog} de SIATE, que tomaba el usuario de la ruta y del cuerpo y dejaba cambiar
 * el ícono de cualquiera. */
@RestController
@RequestMapping("/iconos")
public class IconosController {

    private final ListarIconosUseCase listar;
    private final CrearIconoUseCase crear;
    private final EditarIconoUseCase editar;
    private final EliminarIconoUseCase eliminar;

    public IconosController(ListarIconosUseCase listar, CrearIconoUseCase crear, EditarIconoUseCase editar,
                            EliminarIconoUseCase eliminar) {
        this.listar = listar;
        this.crear = crear;
        this.editar = editar;
        this.eliminar = eliminar;
    }

    @GetMapping
    public IconosResponse listarIconos(Authentication autenticacion) {
        return new IconosResponse(listar.listar(autenticacion.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Icono crearIcono(@RequestBody DatosIcono datos, Authentication autenticacion) {
        return crear.crear(autenticacion.getName(), datos);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarIcono(@PathVariable int id, @RequestBody DatosIcono datos, Authentication autenticacion) {
        editar.editar(id, autenticacion.getName(), datos);
    }

    /** POST y no DELETE, como al eliminar una capa: los únicos verbos que se sabe que WebSEAL deja pasar. */
    @PostMapping("/{id}/eliminar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarIcono(@PathVariable int id, Authentication autenticacion) {
        eliminar.eliminar(id, autenticacion.getName());
    }

    public record IconosResponse(List<Icono> data) {
    }
}
