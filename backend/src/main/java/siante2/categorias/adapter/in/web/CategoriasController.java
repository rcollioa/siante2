package siante2.categorias.adapter.in.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import siante2.categorias.application.port.in.ListarCategoriasUseCase;
import siante2.compartido.adapter.in.web.Catalogo;
import siante2.categorias.domain.Categoria;

/** Reemplaza a {@code GET /siante/api/categories} de SIANTE. */
@RestController
@Catalogo
@RequestMapping("/categorias")
public class CategoriasController {

    private final ListarCategoriasUseCase listarCategorias;

    public CategoriasController(ListarCategoriasUseCase listarCategorias) {
        this.listarCategorias = listarCategorias;
    }

    @GetMapping
    public CategoriasResponse listarCategorias() {
        return new CategoriasResponse(listarCategorias.listarActivas());
    }

    public record CategoriasResponse(List<Categoria> data) {
    }
}
