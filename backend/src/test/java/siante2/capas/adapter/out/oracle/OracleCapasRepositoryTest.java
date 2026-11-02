package siante2.capas.adapter.out.oracle;

import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import siante2.ContadorSql;
import siante2.capas.domain.Capa;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.TipoServicio;
import siante2.capas.domain.Visibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** V_LAYERS y LAYERS_FEATURES en H2, con el JSON_METADATA como CLOB igual que en Oracle. */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(OracleCapasRepository.class)
class OracleCapasRepositoryTest {

    private static final int YO = 7;
    private static final int OTRO = 8;

    @Autowired
    private OracleCapasRepository repositorio;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory fabrica;

    @BeforeEach
    void preparar() {
        jdbc.execute("alter table V_LAYERS alter column JSON_METADATA clob");
        jdbc.execute("alter table LAYERS_FEATURES alter column JSON_METADATA clob");
        jdbc.update("delete from V_LAYERS");
        jdbc.update("delete from LAYERS_FEATURES");
        capa(1, YO, "Bodegas", "0", "1", "{\"type\":\"FeatureCollection\",\"features\":[]}");
        capa(2, YO, "Área de un grupo", "3", "1", "");
        capa(3, YO, "Borrada", "0", "0", "");
        capa(4, YO, "Servicio IDE", "1", "1", "{\"type\":\"WMS\",\"url\":\"https://ide.cl/wms\"}");
        capa(5, OTRO, "Ajena privada", "0", "1", "");
        capa(6, OTRO, "Ajena pública", "1", "1", "");
        capa(7, OTRO, "Formal", "2", "1", null);
        elemento(1, 0, "{\"geometry\":{\"type\":\"Point\",\"coordinates\":[-70.65,-33.44]},\"metadata\":{\"name\":\"A\"}}");
        elemento(1, 1, "{\"geometry\":{\"type\":\"Circle\",\"center\":[-70.6,-33.4],\"radius\":500}}");
        elemento(1, 2, "{\"geometry\":{\"type\":\"Point\"}}");
        elemento(6, 0, "{\"geometry\":{\"type\":\"Point\",\"coordinates\":[-71.6,-33.0]}}");
    }

    @Test
    void listaLasCapasActivasDelUsuarioSinAreasDeDibujoEnUnaSolaConsulta() {
        ContadorSql sql = new ContadorSql(fabrica);

        assertThat(repositorio.listarDe(YO)).containsExactly(
                new ResumenCapa(1, "Bodegas", "Descripción de Bodegas", Visibilidad.PRIVADA),
                new ResumenCapa(4, "Servicio IDE", "Descripción de Servicio IDE", Visibilidad.PUBLICA));
        assertThat(sql.sentencias()).isEqualTo(1);
    }

    @Test
    void unaCapaPropiaTraeSusElementosYOmiteLosInvalidos() {
        ContadorSql sql = new ContadorSql(fabrica);

        Capa capa = repositorio.buscarVisible(1, YO).orElseThrow();

        assertThat(capa.propia()).isTrue();
        assertThat(capa.tipo()).isEqualTo(TipoCapa.VECTORIAL);
        assertThat(capa.elementos()).extracting(ElementoCapa::id, ElementoCapa::radio)
                .containsExactly(tuple(100, 0.0), tuple(101, 500.0));
        assertThat(sql.sentencias()).isEqualTo(2);
    }

    @Test
    void lasCapasPublicasYFormalesDeOtrosSeVenPeroNoSonPropias() {
        assertThat(repositorio.buscarVisible(6, YO)).hasValueSatisfying(capa -> {
            assertThat(capa.propia()).isFalse();
            assertThat(capa.elementos()).hasSize(1);
        });
        assertThat(repositorio.buscarVisible(7, YO)).hasValueSatisfying(
                capa -> assertThat(capa.visibilidad()).isEqualTo(Visibilidad.FORMAL));
    }

    @Test
    void unaCapaPrivadaAjenaOBorradaNoSaleDeLaBase() {
        assertThat(repositorio.buscarVisible(5, YO)).isEmpty();
        assertThat(repositorio.buscarVisible(3, YO)).isEmpty();
        assertThat(repositorio.buscarVisible(99, YO)).isEmpty();
    }

    @Test
    void elServidorDeUnaCapaPorUrlSaleDeSuJsonSoloSiElUsuarioPuedeVerla() {
        assertThat(repositorio.servicioVisible(4, YO))
                .contains(new OrigenServicio(TipoServicio.WMS, "https://ide.cl/wms"));
        assertThat(repositorio.servicioVisible(1, YO)).isEmpty();
        assertThat(repositorio.servicioVisible(5, YO)).isEmpty();
    }

    @Test
    void unaCapaSinFilasEnLayersFeaturesTraeLosElementosDeSuPropioJson() {
        capa(8, YO, "Sin migrar", "0", "1", """
                {"type":"FeatureCollection","features":[
                 {"type":"Feature","geometry":{"type":"Point","coordinates":[-70.65,-33.44]}},
                 {"type":"Feature","geometry":{"type":"LineString","coordinates":[[-70.6,-33.4],[-70.5,-33.3]]}},
                 {"type":"Feature","geometry":{"type":"Point"}}]}""");
        ContadorSql sql = new ContadorSql(fabrica);

        Capa capa = repositorio.buscarVisible(8, YO).orElseThrow();

        assertThat(capa.elementos()).extracting(elemento -> elemento.geometria().type()).containsExactly("Point", "LineString");
        assertThat(sql.sentencias()).isEqualTo(3);
    }

    @Test
    void conFilasEnLayersFeaturesNoSeLeeElJsonDeLaCapa() {
        capa(8, YO, "Migrada", "0", "1", """
                {"type":"FeatureCollection","features":[{"geometry":{"type":"Point","coordinates":[-1.0,-1.0]}}]}""");
        elemento(8, 0, "{\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[-70.6,-33.4],[-70.5,-33.3]]}}");
        ContadorSql sql = new ContadorSql(fabrica);

        Capa capa = repositorio.buscarVisible(8, YO).orElseThrow();

        assertThat(capa.elementos()).extracting(elemento -> elemento.geometria().type()).containsExactly("LineString");
        assertThat(sql.sentencias()).isEqualTo(2);
    }

    @Test
    void unaCapaEnBlancoNoLeeSuJson() {
        ContadorSql sql = new ContadorSql(fabrica);

        assertThat(repositorio.buscarVisible(7, YO).orElseThrow().elementos()).isEmpty();
        assertThat(sql.sentencias()).isEqualTo(2);
    }

    @Test
    void enOracleUnTextoVacioLlegaComoNullYElDominioRecibeTextoVacio() {
        assertThat(new FilaResumenCapa(1, null, null, "0").descripcion()).isEmpty();
        assertThat(new FilaCapaVisible(1, null, null, "0", 7, null).descripcion()).isEmpty();
        FilaCapaCatalogo catalogo = new FilaCapaCatalogo(1, null, null, "1", null, null, null, false);
        assertThat(List.of(catalogo.nombre(), catalogo.descripcion(), catalogo.categoria(), catalogo.nombreDueno(), catalogo.apellidoDueno()))
                .containsOnly("");
    }

    @Test
    void unaCapaPorUrlNoConsultaElementos() {
        ContadorSql sql = new ContadorSql(fabrica);

        Capa capa = repositorio.buscarVisible(4, YO).orElseThrow();

        assertThat(capa.tipo()).isEqualTo(TipoCapa.URL);
        assertThat(capa.elementos()).isEmpty();
        assertThat(sql.sentencias()).isEqualTo(1);
    }

    private void capa(int id, int dueno, String nombre, String visibilidad, String estado, String json) {
        jdbc.update("insert into V_LAYERS (ID, USER_ID, NAME, DESCRIPTION, VISIBILITY, STATUS, JSON_METADATA) "
                + "values (?, ?, ?, ?, ?, ?, ?)", id, dueno, nombre, "Descripción de " + nombre, visibilidad, estado, json);
    }

    /** Numerados desde capa × 100 para distinguirlos en las aserciones (la clave real es ID_FEATURE + ID_LAYER). */
    private void elemento(int capa, int numero, String json) {
        jdbc.update("insert into LAYERS_FEATURES (ID_LAYER, ID_FEATURE, TYPE, JSON_METADATA) values (?, ?, ?, ?)",
                capa, numero + capa * 100, "Point", json);
    }
}
