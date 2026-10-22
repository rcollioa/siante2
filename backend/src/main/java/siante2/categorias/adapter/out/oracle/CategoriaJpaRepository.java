package siante2.categorias.adapter.out.oracle;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import siante2.categorias.domain.Categoria;

interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Integer> {

    /** Proyección directa al dominio: solo las columnas necesarias y sin entidades en la sesión. */
    @Query("""
            select new siante2.categorias.domain.Categoria(c.id, c.name, coalesce(c.description, ''))
            from CategoriaEntity c
            where c.status = :estado
            order by c.name""")
    List<Categoria> buscarPorEstado(String estado);
}
