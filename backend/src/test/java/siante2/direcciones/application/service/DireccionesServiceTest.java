package siante2.direcciones.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.direcciones.domain.Direccion;
import siante2.direcciones.domain.ZonaVisible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DireccionesServiceTest {

    private final List<String> consultados = new ArrayList<>();
    private final List<String> sugeridos = new ArrayList<>();
    private final List<Optional<ZonaVisible>> zonas = new ArrayList<>();
    private final DireccionesService servicio = new DireccionesService(
            texto -> {
                consultados.add(texto);
                return List.of(new Direccion("Teatinos 56, Santiago", -33.44, -70.65));
            },
            (texto, zona) -> {
                sugeridos.add(texto);
                zonas.add(zona);
                return List.of(new Direccion("Teatinos, Santiago", -33.44, -70.65));
            });

    @Test
    void sugiereConLaMismaNormalizacionYLimitesQueLaBusqueda() {
        assertThat(servicio.sugerir("  Teati ", Optional.empty())).hasSize(1);
        assertThatThrownBy(() -> servicio.sugerir("te", Optional.empty())).isInstanceOf(SolicitudInvalidaException.class);

        assertThat(sugeridos).containsExactly("teati");
        assertThat(zonas).containsExactly(Optional.empty());
        assertThat(consultados).isEmpty();
    }

    @Test
    void redondeaLaZonaDelMapaParaQueMoverloUnPocoNoCambieLaConsulta() {
        servicio.sugerir("teatinos", Optional.of(new ZonaVisible(-33.44312, -70.65449, 12)));
        servicio.sugerir("teatinos", Optional.of(new ZonaVisible(-33.4429, -70.6501, 12)));

        assertThat(zonas).containsExactly(
                Optional.of(new ZonaVisible(-33.44, -70.65, 12)), Optional.of(new ZonaVisible(-33.44, -70.65, 12)));
    }

    @Test
    void rechazaUnaZonaFueraDeRangoSinConsultar() {
        for (ZonaVisible invalida : List.of(new ZonaVisible(-91, -70.65, 12), new ZonaVisible(-33.44, 181, 12),
                new ZonaVisible(Double.NaN, -70.65, 12), new ZonaVisible(-33.44, -70.65, -1),
                new ZonaVisible(-33.44, -70.65, DireccionesService.ZOOM_MAXIMO + 1))) {
            assertThatThrownBy(() -> servicio.sugerir("teatinos", Optional.of(invalida)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("La zona del mapa no es válida.");
        }
        assertThat(sugeridos).isEmpty();
    }

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
