package siante2.compartido.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import siante2.compartido.domain.Geometria;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** El mixin da a Geometria la forma GeoJSON sin anotar el dominio. */
@JsonTest
class GeometriaJsonMixinTest {

    @Autowired
    private JsonMapper json;

    @Test
    void omiteLosCamposQueNoAplicanSegunElTipo() {
        assertThat(json.writeValueAsString(Geometria.simple("Point", List.of(-70.6, -33.4))))
                .isEqualTo("{\"type\":\"Point\",\"coordinates\":[-70.6,-33.4]}");
        assertThat(json.writeValueAsString(Geometria.coleccion(List.of(Geometria.simple("Point", List.of(1.0, 2.0))))))
                .isEqualTo("{\"type\":\"GeometryCollection\",\"geometries\":[{\"type\":\"Point\",\"coordinates\":[1.0,2.0]}]}");
    }
}
