package siante2.capas.adapter.out.oracle;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** El dueño va en el where de cada cambio: un ícono ajeno o del sistema no se toca y no se distingue de uno que no existe. */
interface IconoJpaRepository extends Repository<PuntoCatalogoEntity, Integer> {

    PuntoCatalogoEntity save(PuntoCatalogoEntity icono);

    /** Con el CLOB de la imagen, a diferencia de otros listados: sin él no se puede mostrar el catálogo. Son pocas filas y
     * cada imagen es pequeña. */
    @Query("""
            select new siante2.capas.adapter.out.oracle.FilaIcono(
                i.id, coalesce(i.name, ''), coalesce(i.nameImage, ''), i.nameFile, i.owner)
            from PuntoCatalogoEntity i
            where i.owner in (:usuario, :sistema) and i.visibility = :activo
            order by i.id""")
    List<FilaIcono> listar(int usuario, int sistema, String activo);

    @Modifying
    @Query("""
            update PuntoCatalogoEntity i
            set i.name = :nombre, i.nameImage = :archivo, i.nameFile = :imagen, i.url = null
            where i.id = :id and i.owner = :dueno and i.visibility = :activo""")
    int actualizar(int id, int dueno, String nombre, String archivo, String imagen, String activo);

    @Modifying
    @Query("""
            update PuntoCatalogoEntity i
            set i.visibility = :inactivo
            where i.id = :id and i.owner = :dueno and i.visibility = :activo""")
    int desactivar(int id, int dueno, String inactivo, String activo);
}
