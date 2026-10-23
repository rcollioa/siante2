package siante2.multimedia.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.multimedia.application.port.in.ListarMultimediaUseCase;
import siante2.multimedia.application.port.in.ObtenerArchivoMultimediaUseCase;
import siante2.multimedia.application.port.out.MultimediaReadPort;
import siante2.multimedia.domain.ArchivoMultimedia;
import siante2.multimedia.domain.Multimedia;

@Service
public class MultimediaService implements ListarMultimediaUseCase, ObtenerArchivoMultimediaUseCase {

    private final MultimediaReadPort multimedia;

    public MultimediaService(MultimediaReadPort multimedia) {
        this.multimedia = multimedia;
    }

    @Override
    public List<Multimedia> listar() {
        return multimedia.listar();
    }

    @Override
    public ArchivoMultimedia obtener(int id) {
        return multimedia.contenido(id)
                .filter(c -> c.bytes() != null && c.bytes().length > 0)
                .map(c -> ArchivoMultimedia.desde(c.descripcion(), c.bytes()))
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el archivo multimedia " + id + "."));
    }
}
