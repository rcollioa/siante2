package siante2.capas.adapter.out.oracle;

import java.util.List;
import org.junit.jupiter.api.Test;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.compartido.domain.Geometria;

import static org.assertj.core.api.Assertions.assertThat;

/** Ejemplos con la forma exacta en que SIATE guarda los elementos ({@code convertOlToJsonMetadata}). */
class ElementoSiateTest {

    @Test
    void leeUnMarcadorConSuIconoNombreDescripcionYMetadatos() {
        String json = """
                {"type":"Feature","geometry":{"type":"Point","coordinates":[-70.6539,-33.4429],
                 "creationdate":"27/10/2026 10:15:00"},
                 "properties":{"image":"data:image/svg+xml;base64,PHN2Zy8+"},
                 "metadata":{"code":"0","name":"Contraloría","description":"Oficina central",
                  "metadata":[{"code":"0","name":"Dirección","description":"Teatinos 56"},
                              {"code":"0","name":"Pisos","description":12},{"code":"0","name":" ","description":"x"}]}}""";

        assertThat(ElementoSiate.leer(3, json)).contains(new ElementoCapa(3,
                Geometria.simple("Point", List.of(-70.6539, -33.4429)), 0, "Contraloría", "Oficina central",
                new EstiloElemento("", 0, "", "data:image/svg+xml;base64,PHN2Zy8+"),
                List.of(new Metadato("Dirección", "Teatinos 56"), new Metadato("Pisos", "12"))));
    }

    @Test
    void unCirculoQuedaComoSuCentroConElRadio() {
        String json = """
                {"type":"Feature","geometry":{"type":"Circle","center":[-70.65,-33.44],"radius":1250.5},
                 "properties":{"color":"#3399CC","width":3,"fill":"rgba(255,255,255,0.4)"}}""";

        ElementoCapa circulo = ElementoSiate.leer(0, json).orElseThrow();

        assertThat(circulo.geometria()).isEqualTo(Geometria.simple("Point", List.of(-70.65, -33.44)));
        assertThat(circulo.radio()).isEqualTo(1250.5);
        assertThat(circulo.estilo()).isEqualTo(new EstiloElemento("#3399CC", 3, "rgba(255,255,255,0.4)", ""));
        assertThat(circulo.nombre()).isEmpty();
        assertThat(circulo.metadatos()).isEmpty();
    }

    @Test
    void losColoresComoArregloDeOpenLayersPasanATextoCss() {
        String json = """
                {"geometry":{"type":"polygon","coordinates":[[[-70,-33],[-70.1,-33],[-70.1,-33.1],[-70,-33]]]},
                 "properties":{"color":[255,0,0,1],"fill":[0,0,255,0.25],"width":2.5}}""";

        ElementoCapa poligono = ElementoSiate.leer(1, json).orElseThrow();

        assertThat(poligono.geometria().type()).isEqualTo("Polygon");
        assertThat(poligono.geometria().coordinates()).isEqualTo(
                List.of(List.of(List.of(-70.0, -33.0), List.of(-70.1, -33.0), List.of(-70.1, -33.1), List.of(-70.0, -33.0))));
        assertThat(poligono.estilo()).isEqualTo(new EstiloElemento("rgba(255, 0, 0, 1)", 2.5, "rgba(0, 0, 255, 0.25)", ""));
    }

    @Test
    void omiteLoQueNoSePuedeDibujar() {
        assertThat(ElementoSiate.leer(1, null)).isEmpty();
        assertThat(ElementoSiate.leer(1, " ")).isEmpty();
        assertThat(ElementoSiate.leer(1, "{no es json")).isEmpty();
        assertThat(ElementoSiate.leer(1, "{\"type\":\"Feature\"}")).isEmpty();
        assertThat(ElementoSiate.leer(1, "{\"geometry\":{\"type\":\"Spline\",\"coordinates\":[1,2]}}")).isEmpty();
        assertThat(ElementoSiate.leer(1, "{\"geometry\":{\"type\":\"Point\",\"coordinates\":[]}}")).isEmpty();
        assertThat(ElementoSiate.leer(1, "{\"geometry\":{\"type\":\"Point\",\"coordinates\":[[1,2]]}}")).isEmpty();
        assertThat(ElementoSiate.leer(1, "{\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[1,null]]}}")).isEmpty();
        assertThat(ElementoSiate.leer(1, "{\"geometry\":{\"type\":\"Circle\",\"center\":[1,2]}}")).isEmpty();
    }
}
