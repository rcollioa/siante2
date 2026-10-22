package siante2.categorias.application.port.in;

import java.util.List;
import siante2.categorias.domain.Categoria;

public interface ListarCategoriasUseCase {

    /** Categorías activas, ordenadas por nombre. */
    List<Categoria> listarActivas();
}
