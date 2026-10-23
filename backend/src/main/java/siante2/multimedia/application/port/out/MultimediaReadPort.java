package siante2.multimedia.application.port.out;

import java.util.List;
import java.util.Optional;
import siante2.multimedia.domain.Multimedia;

public interface MultimediaReadPort {

    List<Multimedia> listar();

    Optional<Contenido> contenido(int id);

    record Contenido(String descripcion, byte[] bytes) {
    }
}
