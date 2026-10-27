package siante2.multimedia.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import siante2.compartido.adapter.in.web.Catalogo;
import siante2.multimedia.application.port.in.ListarMultimediaUseCase;
import siante2.multimedia.application.port.in.ObtenerArchivoMultimediaUseCase;
import siante2.multimedia.domain.ArchivoMultimedia;
import siante2.multimedia.domain.Multimedia;

/** Reemplaza a {@code GET /siante/api/multimedia/getAll} de SIATE, que entregaba todos los archivos en base64. */
@RestController
@Catalogo
@RequestMapping("/multimedia")
public class MultimediaController {

    private final ListarMultimediaUseCase listar;
    private final ObtenerArchivoMultimediaUseCase obtenerArchivo;

    public MultimediaController(ListarMultimediaUseCase listar, ObtenerArchivoMultimediaUseCase obtenerArchivo) {
        this.listar = listar;
        this.obtenerArchivo = obtenerArchivo;
    }

    @GetMapping
    public MultimediaResponse listarMultimedia() {
        return new MultimediaResponse(listar.listar());
    }

    /** Imágenes en línea (para {@code <img>}); el resto, como descarga con su nombre. Sin ETag: calcularlo
     * obligaría a copiar el archivo completo en memoria en cada descarga. */
    @GetMapping(value = "/{id}/archivo", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_GIF_VALUE,
            MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<byte[]> obtenerArchivoMultimedia(@PathVariable int id, HttpServletRequest request) {
        ShallowEtagHeaderFilter.disableContentCaching(request);
        ArchivoMultimedia archivo = obtenerArchivo.obtener(id);
        ContentDisposition disposicion = (archivo.esImagen() ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(archivo.nombre(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(archivo.contenido());
    }

    public record MultimediaResponse(List<Multimedia> data) {
    }
}
