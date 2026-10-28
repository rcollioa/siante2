package siante2.capas.adapter.out.oracle;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** SIATE numera {@code ID_FEATURE} dentro de cada capa ({@code LayerService.insertFeatures}), así que un elemento
 * se identifica con su capa ({@link ElementoCapaEntity.Clave}). Se lee siempre por capa y con proyección. */
interface ElementoCapaJpaRepository extends Repository<ElementoCapaEntity, ElementoCapaEntity.Clave> {

    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaElementoCapa(coalesce(e.idFeature, 0), e.jsonMetadata)
            from ElementoCapaEntity e
            where e.idLayer = :capa
            order by e.idFeature""")
    List<FilaElementoCapa> buscarPorCapa(int capa);
}
