package siante2.multimedia.application.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.multimedia.application.port.out.MultimediaReadPort;
import siante2.multimedia.domain.Multimedia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultimediaServiceTest {

    private final MultimediaService servicio = new MultimediaService(new MultimediaReadPort() {
        private final Map<Integer, Contenido> contenidos = Map.of(
                1, new Contenido("Manual SIATE", "%PDF-1.7".getBytes(StandardCharsets.ISO_8859_1)),
                2, new Contenido("Vacío", new byte[0]));

        @Override
        public List<Multimedia> listar() {
            return List.of(new Multimedia(1, "Manual SIATE"));
        }

        @Override
        public Optional<Contenido> contenido(int id) {
            return Optional.ofNullable(contenidos.get(id));
        }
    });

    @Test
    void entregaElArchivoConSuTipo() {
        assertThat(servicio.obtener(1).nombre()).isEqualTo("Manual_SIATE.pdf");
    }

    @Test
    void unArchivoInexistenteOVacioEsNoEncontrado() {
        assertThatThrownBy(() -> servicio.obtener(9)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.obtener(2)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
