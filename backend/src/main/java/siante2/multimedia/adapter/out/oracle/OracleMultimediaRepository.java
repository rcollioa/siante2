package siante2.multimedia.adapter.out.oracle;

import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.multimedia.application.port.out.MultimediaReadPort;
import siante2.multimedia.domain.Multimedia;

/** Los archivos de ayuda casi no cambian: listado y contenido quedan en la caché del servidor. */
@Repository
@Transactional(readOnly = true)
class OracleMultimediaRepository implements MultimediaReadPort {

    private final MultimediaJpaRepository jpa;

    OracleMultimediaRepository(MultimediaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Cacheable("multimedia")
    public List<Multimedia> listar() {
        return jpa.listar();
    }

    @Override
    @Cacheable("multimedia-contenido")
    public Optional<Contenido> contenido(int id) {
        return jpa.contenido(id);
    }
}
