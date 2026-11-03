package siante2.capas.adapter.out.oracle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InicioJsonCapaTest {

    @Test
    void reconoceLasCapasPorUrlTambienDentroDeUnArreglo() {
        assertThat(InicioJsonCapa.esPorUrl("{\"type\":\"WMS\",\"url\":\"https://ide.cl\"}")).isTrue();
        assertThat(InicioJsonCapa.esPorUrl("[{\"type\" : \"wfs\",\"url\":\"https://ide.cl\"}]")).isTrue();
        assertThat(InicioJsonCapa.esPorUrl("{\"type\":\"FeatureCollection\",\"features\":[]}")).isFalse();
        assertThat(InicioJsonCapa.esPorUrl(null)).isFalse();
    }

    @Test
    void unElementoQueSeLlamaIgualNoConvierteLaCapaEnPorUrl() {
        assertThat(InicioJsonCapa.esPorUrl(
                "{\"type\":\"FeatureCollection\",\"features\":[{\"metadata\":{\"type\":\"WMS\"}}]}")).isFalse();
    }

    @Test
    void seDibujaEnLasCapasEnBlancoYEnLasColeccionesPropias() {
        assertThat(InicioJsonCapa.sePuedeDibujar(null)).isTrue();
        assertThat(InicioJsonCapa.sePuedeDibujar("")).isTrue();
        assertThat(InicioJsonCapa.sePuedeDibujar("{\"type\":\"FeatureCollection\",\"zoom\":4,\"features\":[]}")).isTrue();
    }

    @Test
    void noSeDibujaEnLasCapasPorUrlNiEnLasColeccionesQueSeSincronizan() {
        assertThat(InicioJsonCapa.sePuedeDibujar("{\"type\":\"WMTS\",\"url\":\"https://ide.cl\"}")).isFalse();
        assertThat(InicioJsonCapa.sePuedeDibujar(
                "{\"type\":\"FeatureCollection\",\"zoom\":4,\"url\":\"https://ide.cl/kml\",\"features\":[]}")).isFalse();
    }

    @Test
    void unDatoLlamadoUrlEnUnElementoNoImpideDibujar() {
        assertThat(InicioJsonCapa.sePuedeDibujar(
                "{\"type\":\"FeatureCollection\",\"features\":[{\"properties\":{\"url\":\"https://x.cl\"}}]}")).isTrue();
    }
}
