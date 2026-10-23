package siante2.multimedia.application.port.in;

import siante2.multimedia.domain.ArchivoMultimedia;

public interface ObtenerArchivoMultimediaUseCase {

    /** Contenido del archivo; {@code RecursoNoEncontradoException} si no existe o está vacío. */
    ArchivoMultimedia obtener(int id);
}
