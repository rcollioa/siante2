package siante2.multimedia.domain;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchivoMultimediaTest {

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void detectaElTipoPorLosPrimerosBytesYArmaElNombre() {
        ArchivoMultimedia manual = ArchivoMultimedia.desde("Manual SIATE", bytes("%PDF-1.7 ..."));
        ArchivoMultimedia gif = ArchivoMultimedia.desde("Gif Metadata", bytes("GIF89a..."));
        ArchivoMultimedia png = ArchivoMultimedia.desde("Logo", new byte[] {(byte) 0x89, 'P', 'N', 'G', 1});
        ArchivoMultimedia otro = ArchivoMultimedia.desde(" ", bytes("zzzz"));

        assertThat(manual.nombre()).isEqualTo("Manual_SIATE.pdf");
        assertThat(manual.tipoContenido()).isEqualTo("application/pdf");
        assertThat(manual.esImagen()).isFalse();
        assertThat(gif.tipoContenido()).isEqualTo("image/gif");
        assertThat(gif.esImagen()).isTrue();
        assertThat(png.nombre()).isEqualTo("Logo.png");
        assertThat(otro.nombre()).isEqualTo("archivo");
        assertThat(otro.tipoContenido()).isEqualTo("application/octet-stream");
    }
}
