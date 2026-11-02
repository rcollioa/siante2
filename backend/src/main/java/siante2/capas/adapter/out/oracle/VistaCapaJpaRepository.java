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

    /** El JSON completo de una capa que el usuario puede ver. Solo si hace falta: el de una capa por URL es corto, pero
     * el de una vectorial sin filas en {@code LAYERS_FEATURES} puede pesar megas. */
    @Query("""
            select c.jsonMetadata
            from VistaCapaEntity c
            where c.id = :capa and c.status = :activa and (c.userId = :usuario or c.visibility in :deTodos)""")
    Optional<String> buscarJsonVisible(int capa, int usuario, String activa, Collection<String> deTodos);

    /** Como SIATE: el favorito apunta a un grupo ({@code FAVORITES.L_GROUP_ID}) y sus capas salen de
     * {@code LAYERS_GROUPS_LAYERS}. Además, solo las que el usuario todavía puede ver. */
    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaResumenCapa(
                c.id, coalesce(c.name, ''), coalesce(c.description, ''), coalesce(c.visibility, '0'))
            from VistaCapaEntity c
            where c.status = :activa and coalesce(c.visibility, '0') <> :areaDeDibujo
              and (c.userId = :usuario or c.visibility in :deTodos)
              and c.id in (select gl.layerId from GrupoCapasCapaEntity gl, FavoritoEntity f
                         where gl.lGroupId = f.lGroupId and f.userId = :usuario)
            order by c.id""")
    List<FilaResumenCapa> buscarFavoritasDe(int usuario, String activa, String areaDeDibujo, Collection<String> deTodos);

    /** Pestaña "Todas" del catálogo: las capas públicas de cualquier usuario. */
    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaCapaCatalogo(
                c.id, coalesce(c.name, ''), coalesce(c.description, ''), coalesce(c.visibility, '0'),
                coalesce(c.categoryName, ''), coalesce(c.userName, ''), coalesce(c.userLastName, ''),
                case when c.id in (select gl.layerId from GrupoCapasCapaEntity gl, FavoritoEntity f
                         where gl.lGroupId = f.lGroupId and f.userId = :usuario) then true else false end)
            from VistaCapaEntity c
            where c.status = :activa and c.visibility = :publica
            order by c.id""")
    List<FilaCapaCatalogo> catalogoPublico(int usuario, String activa, String publica);

    /** Pestañas "Usuario" (dueño = usuario) y "Capas formales" (dueño = usuario de sistema de SIATE). De otro dueño,
     * solo las públicas o formales: una capa privada ajena no sale de la base. */
    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaCapaCatalogo(
                c.id, coalesce(c.name, ''), coalesce(c.description, ''), coalesce(c.visibility, '0'),
                coalesce(c.categoryName, ''), coalesce(c.userName, ''), coalesce(c.userLastName, ''),
                case when c.id in (select gl.layerId from GrupoCapasCapaEntity gl, FavoritoEntity f
                         where gl.lGroupId = f.lGroupId and f.userId = :usuario) then true else false end)
            from VistaCapaEntity c
            where c.userId = :dueno and c.status = :activa and coalesce(c.visibility, '0') <> :areaDeDibujo
              and (c.userId = :usuario or c.visibility in :deTodos)
            order by c.id""")
    List<FilaCapaCatalogo> catalogoDe(int dueno, int usuario, String activa, String areaDeDibujo,
                                      Collection<String> deTodos);
}
