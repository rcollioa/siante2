package siante2.categorias.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import siante2.categorias.application.port.in.ListarCategoriasUseCase;
import siante2.categorias.application.port.out.CategoriasReadPort;
import siante2.categorias.domain.Categoria;

@Service
public class CategoriasService implements ListarCategoriasUseCase {

    private final CategoriasReadPort categorias;

    public CategoriasService(CategoriasReadPort categorias) {
        this.categorias = categorias;
    }

    @Override
    public List<Categoria> listarActivas() {
        return categorias.buscarActivas();
    }
}
