package siante2.categorias.application.port.out;

import java.util.List;
import siante2.categorias.domain.Categoria;

public interface CategoriasReadPort {

    List<Categoria> buscarActivas();
}
