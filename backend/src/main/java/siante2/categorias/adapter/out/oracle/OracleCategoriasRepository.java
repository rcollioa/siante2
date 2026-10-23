package siante2.categorias.adapter.out.oracle;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.categorias.application.port.out.CategoriasReadPort;
import siante2.categorias.domain.Categoria;

@Repository
@Transactional(readOnly = true)
class OracleCategoriasRepository implements CategoriasReadPort {

    private final CategoriaJpaRepository jpa;

    OracleCategoriasRepository(CategoriaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Cacheable("categorias")
    public List<Categoria> buscarActivas() {
        return jpa.buscarPorEstado(CategoriaEntity.ACTIVA);
    }
}
