package siante2.direcciones.application.service;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.direcciones.domain.Direccion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DireccionesServiceTest {

    private final List<String> consultados = new ArrayList<>();
    private final DireccionesService servicio = new DireccionesService(texto -> {
        consultados.add(texto);
        return List.of(new Direccion("Teatinos 56, Santiago", -33.44, -70.65));
    });

    @Test
    void normalizaEspaciosYMayusculasAntesDeConsultar() {
        assertThat(servicio.buscar("  Teatinos   56 ")).hasSize(1);
        assertThat(servicio.buscar("teatinos 56")).hasSize(1);

        assertThat(consultados).containsExactly("teatinos 56", "teatinos 56");
    }

    @Test
    void rechazaTextosMuyCortosOMuyLargosSinConsultar() {
        assertThatThrownBy(() -> servicio.buscar(" ab ")).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.buscar(null)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.buscar("a".repeat(DireccionesService.LARGO_MAXIMO + 1)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("La búsqueda debe tener entre 3 y 200 caracteres.");
        assertThat(consultados).isEmpty();
    }
}
