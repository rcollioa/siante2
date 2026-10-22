package siante2.categorias.adapter.out.oracle;

import java.util.List;
import org.springframework.stereotype.Repository;
import siante2.categorias.application.port.out.CategoriasReadPort;
import siante2.categorias.domain.Categoria;

@Repository
class OracleCategoriasRepository implements CategoriasReadPort {

    private final CategoriaJpaRepository jpa;

    OracleCategoriasRepository(CategoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Categoria> buscarActivas() {
        return jpa.buscarPorEstado(CategoriaEntity.ACTIVA).stream()
                .map(c -> new Categoria(c.getId(), c.getNombre(), c.getDescripcion()))
                .toList();
    }
}
