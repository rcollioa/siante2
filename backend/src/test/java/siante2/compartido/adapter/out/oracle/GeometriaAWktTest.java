package siante2.compartido.adapter.out.oracle;

import java.util.List;
import org.junit.jupiter.api.Test;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.SolicitudInvalidaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeometriaAWktTest {

    private static final List<List<Double>> CUADRADO =
            List.of(List.of(0.0, 0.0), List.of(1.0, 0.0), List.of(1.0, 1.0), List.of(0.0, 0.0));

    @Test
    void escribeLosTiposDeGeoJson() {
        assertThat(GeometriaAWkt.escribir(Geometria.simple("Point", List.of(-70.5, -33.4))))
                .isEqualTo("POINT (-70.5 -33.4)");
        assertThat(GeometriaAWkt.escribir(Geometria.simple("Polygon", List.of(CUADRADO))))
                .isEqualTo("POLYGON ((0.0 0.0, 1.0 0.0, 1.0 1.0, 0.0 0.0))");
        assertThat(GeometriaAWkt.escribir(Geometria.simple("MultiPoint", List.of(List.of(1, 2), List.of(3, 4)))))
                .isEqualTo("MULTIPOINT ((1.0 2.0), (3.0 4.0))");
        assertThat(GeometriaAWkt.escribir(Geometria.coleccion(List.of(Geometria.simple("Point", List.of(1, 2))))))
                .isEqualTo("GEOMETRYCOLLECTION (POINT (1.0 2.0))");
    }

    @Test
    void corrigeLaOrientacionExteriorAntihorarioYHuecoHorario() {
        List<List<Double>> exteriorHorario = CUADRADO.reversed();
        List<List<Double>> huecoAntihorario = List.of(List.of(0.2, 0.2), List.of(0.4, 0.2), List.of(0.4, 0.4), List.of(0.2, 0.2));

        assertThat(GeometriaAWkt.escribir(Geometria.simple("Polygon", List.of(exteriorHorario, huecoAntihorario))))
                .isEqualTo("POLYGON ((0.0 0.0, 1.0 0.0, 1.0 1.0, 0.0 0.0), (0.2 0.2, 0.4 0.4, 0.4 0.2, 0.2 0.2))");
    }

    @Test
    void rechazaGeometriasInvalidasComoSolicitudInvalida() {
        assertThatThrownBy(() -> GeometriaAWkt.escribir(Geometria.simple("Point", List.of(200, 0))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("fuera de rango");
        assertThatThrownBy(() -> GeometriaAWkt.escribir(Geometria.simple("Polygon",
                List.of(List.of(List.of(0, 0), List.of(1, 0), List.of(1, 1), List.of(0, 1))))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("cerrarse");
        assertThatThrownBy(() -> GeometriaAWkt.escribir(Geometria.simple("LineString", List.of(List.of(0, 0)))))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> GeometriaAWkt.escribir(Geometria.simple("Point", List.of("1); DROP TABLE x", 0))))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> GeometriaAWkt.escribir(Geometria.simple("Curve", List.of())))
                .isInstanceOf(SolicitudInvalidaException.class);
    }
}
