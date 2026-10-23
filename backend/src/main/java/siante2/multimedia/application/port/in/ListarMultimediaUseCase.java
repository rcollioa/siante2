package siante2.multimedia.application.port.in;

import java.util.List;
import siante2.multimedia.domain.Multimedia;

public interface ListarMultimediaUseCase {

    /** Archivos disponibles, ordenados por descripción; sin su contenido. */
    List<Multimedia> listar();
}
