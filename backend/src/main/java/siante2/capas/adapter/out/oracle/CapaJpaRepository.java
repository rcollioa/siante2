package siante2.capas.adapter.out.oracle;

import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Escrituras en {@code LAYERS}, que SIATE también escribe: cada una cambia solo sus columnas, sin cargar la entidad ni
 * su {@code JSON_METADATA}. El dueño va en el where: una capa ajena no se toca y no se distingue de una que no existe. */
interface CapaJpaRepository extends Repository<CapaEntity, Integer> {

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
}
