package siante2.capas.adapter.out.oracle;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Escrituras en {@code LAYERS}, que SIATE también escribe: cada una cambia solo sus columnas, sin cargar la entidad ni
 * su {@code JSON_METADATA}. El dueño va en el where: una capa ajena no se toca y no se distingue de una que no existe. */
interface CapaJpaRepository extends Repository<CapaEntity, Integer> {

    CapaEntity save(CapaEntity capa);

    @Modifying
    @Query("""
            update CapaEntity c
            set c.name = :nombre, c.description = :descripcion, c.categoryId = :categoria, c.visibility = :visibilidad
            where c.id = :capa and c.owner = :dueno and c.status = :activa and coalesce(c.visibility, '0') in :gestionables""")
    int actualizar(int capa, int dueno, String nombre, String descripcion, int categoria, String visibilidad, String activa,
                   Collection<String> gestionables);

    @Modifying
    @Query("""
            update CapaEntity c
            set c.visibility = :visibilidad
            where c.id = :capa and c.owner = :dueno and c.status = :activa and coalesce(c.visibility, '0') in :gestionables""")
    int cambiarVisibilidad(int capa, int dueno, String visibilidad, String activa, Collection<String> gestionables);

    @Modifying
    @Query("""
            update CapaEntity c
            set c.status = :inactiva
            where c.id = :capa and c.owner = :dueno and c.status = :activa and coalesce(c.visibility, '0') in :gestionables""")
    int desactivar(int capa, int dueno, String inactiva, String activa, Collection<String> gestionables);

    /** Sin cambiar nada: verifica el dueño y deja la fila bloqueada hasta el fin de la transacción, para que dos
     * guardados de la misma capa no calculen el mismo {@code ID_FEATURE}. */
    @Modifying
    @Query("""
            update CapaEntity c
            set c.status = :activa
            where c.id = :capa and c.owner = :dueno and c.status = :activa and coalesce(c.visibility, '0') in :gestionables""")
    int bloquearParaEditar(int capa, int dueno, String activa, Collection<String> gestionables);

    @Query("select substring(c.jsonMetadata, 1, " + InicioJsonCapa.LARGO + ") from CapaEntity c where c.id = :capa")
    Optional<String> inicioJson(int capa);

    @Query("select c.jsonMetadata from CapaEntity c where c.id = :capa")
    Optional<String> jsonDe(int capa);
}
