package siante2.multimedia.adapter.out.oracle;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import siante2.multimedia.application.port.out.MultimediaReadPort.Contenido;
import siante2.multimedia.domain.Multimedia;

interface MultimediaJpaRepository extends JpaRepository<MultimediaEntity, Integer> {

    /** Sin la columna ARCHIVO: el listado no trae los BLOB. */
    @Query("""
            select new siante2.multimedia.domain.Multimedia(m.xMultimedia, coalesce(m.descripcion, ''))
            from MultimediaEntity m
            order by m.descripcion""")
    List<Multimedia> listar();

    @Query("""
            select new siante2.multimedia.application.port.out.MultimediaReadPort$Contenido(m.descripcion, m.archivo)
            from MultimediaEntity m
            where m.xMultimedia = :id""")
    Optional<Contenido> contenido(int id);
}
