package siante2.capas.adapter.out.oracle;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** {@code V_LAYERS} solo con proyecciones (la vista no tiene clave primaria, ver README). Ninguna consulta trae el
 * {@code JSON_METADATA} completo: es un CLOB de hasta 1.800.000 caracteres por capa. */
interface VistaCapaJpaRepository extends Repository<VistaCapaEntity, Integer> {

    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaResumenCapa(
                c.id, coalesce(c.name, ''), coalesce(c.description, ''), coalesce(c.visibility, '0'))
            from VistaCapaEntity c
            where c.userId = :usuario and c.status = :activa and coalesce(c.visibility, '0') <> :areaDeDibujo
            order by c.id""")
    List<FilaResumenCapa> buscarDe(int usuario, String activa, String areaDeDibujo);

    /** La visibilidad va en el where (README "Autorización"): una capa privada ajena no sale de la base. */
    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaCapaVisible(
                c.id, coalesce(c.name, ''), coalesce(c.description, ''), coalesce(c.visibility, '0'), c.userId,
                substring(c.jsonMetadata, 1, 100))
            from VistaCapaEntity c
            where c.id = :capa and c.status = :activa and (c.userId = :usuario or c.visibility in :deTodos)
            limit 1""")
    Optional<FilaCapaVisible> buscarVisible(int capa, int usuario, String activa, Collection<String> deTodos);
}
