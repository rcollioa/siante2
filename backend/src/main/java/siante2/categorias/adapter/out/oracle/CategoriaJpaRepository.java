package siante2.categorias.adapter.out.oracle;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Integer> {

    @Query("select c from CategoriaEntity c where c.estado = :estado order by c.nombre")
    List<CategoriaEntity> buscarPorEstado(String estado);
}
