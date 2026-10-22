package siante2.compartido.adapter.out.oracle;

import java.util.List;
import org.junit.jupiter.api.Test;
import siante2.compartido.domain.Geometria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** GML con la forma que devuelve {@code SDO_UTIL.TO_GMLGEOMETRY}. */
class GmlAGeometriaTest {

    private static final String NS = " srsName=\"SDO:4326\" xmlns:gml=\"http://www.opengis.net/gml\"";
    private static final String COORD = "<gml:coordinates decimal=\".\" cs=\",\" ts=\" \">";

    @Test
    void punto() {
        Geometria g = GmlAGeometria.leer("<gml:Point" + NS + ">" + COORD + "-70.65,-33.45 </gml:coordinates></gml:Point>");

        assertThat(g).isEqualTo(Geometria.simple("Point", List.of(-70.65, -33.45)));
    }

    @Test
    void poligonoConHuecoDejaElExteriorPrimero() {
        String gml = "<gml:Polygon" + NS + ">"
                + "<gml:innerBoundaryIs><gml:LinearRing>" + COORD + "1,1 2,1 2,2 1,1 </gml:coordinates></gml:LinearRing></gml:innerBoundaryIs>"
                + "<gml:outerBoundaryIs><gml:LinearRing>" + COORD + "0,0 10,0 10,10 0,0 </gml:coordinates></gml:LinearRing></gml:outerBoundaryIs>"
                + "</gml:Polygon>";

        Geometria g = GmlAGeometria.leer(gml);

        assertThat(g.type()).isEqualTo("Polygon");
        assertThat(g.coordinates()).isEqualTo(List.of(
                List.of(List.of(0.0, 0.0), List.of(10.0, 0.0), List.of(10.0, 10.0), List.of(0.0, 0.0)),
                List.of(List.of(1.0, 1.0), List.of(2.0, 1.0), List.of(2.0, 2.0), List.of(1.0, 1.0))));
    }

    @Test
    void multiLineaYColeccion() {
        String linea = "<gml:LineString>" + COORD + "0,0 1,1</gml:coordinates></gml:LineString>";
        Geometria multi = GmlAGeometria.leer("<gml:MultiLineString" + NS + "><gml:lineStringMember>" + linea
                + "</gml:lineStringMember></gml:MultiLineString>");
        Geometria coleccion = GmlAGeometria.leer("<gml:MultiGeometry" + NS + "><gml:geometryMember><gml:Point>" + COORD
                + "5,6</gml:coordinates></gml:Point></gml:geometryMember></gml:MultiGeometry>");

        assertThat(multi.coordinates()).isEqualTo(List.of(List.of(List.of(0.0, 0.0), List.of(1.0, 1.0))));
        assertThat(coleccion.geometries()).containsExactly(Geometria.simple("Point", List.of(5.0, 6.0)));
    }

    @Test
    void separadoresDistintosYPosListDeGml3() {
        Geometria conSeparadores = GmlAGeometria.leer("<gml:LineString" + NS + "><gml:coordinates decimal=\",\" cs=\";\" ts=\"|\">"
                + "1,5;2|3;4,5</gml:coordinates></gml:LineString>");
        Geometria gml3 = GmlAGeometria.leer("<gml:LineString" + NS + "><gml:posList srsDimension=\"2\">1 2 3 4</gml:posList></gml:LineString>");

        assertThat(conSeparadores.coordinates()).isEqualTo(List.of(List.of(1.5, 2.0), List.of(3.0, 4.5)));
        assertThat(gml3.coordinates()).isEqualTo(List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)));
    }

    @Test
    void sinGeometriaDevuelveNullYUnGmlInvalidoFalla() {
        assertThat(GmlAGeometria.leer(null)).isNull();
        assertThatThrownBy(() -> GmlAGeometria.leer("<gml:Curve" + NS + "/>")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GmlAGeometria.leer("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
