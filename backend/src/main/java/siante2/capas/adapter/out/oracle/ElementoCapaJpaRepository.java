package siante2.capas.adapter.out.oracle;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Sin {@code findById}: SIATE numera {@code ID_FEATURE} dentro de cada capa ({@code LayerService.insertFeatures}),
 * así que por sí solo no identifica a un elemento. Se lee siempre por capa y con proyección. */
interface ElementoCapaJpaRepository extends Repository<ElementoCapaEntity, Integer> {

    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaElementoCapa(coalesce(e.idFeature, 0), e.jsonMetadata)
            from ElementoCapaEntity e
            where e.idLayer = :capa
            order by e.idFeature""")
    List<FilaElementoCapa> buscarPorCapa(int capa);
}
