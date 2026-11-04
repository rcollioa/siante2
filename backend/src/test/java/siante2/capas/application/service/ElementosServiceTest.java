package siante2.capas.application.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.capas.application.port.out.ElementosWritePort;
import siante2.capas.domain.CambiosElementos;
import siante2.capas.domain.CapaImportada;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.OrigenArchivo;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ElementosServiceTest {

    private static final Geometria PUNTO = Geometria.simple("Point", List.of(-70.65, -33.44));

    private final EscrituraEnMemoria escritura = new EscrituraEnMemoria();
    private final IdentificarUsuarioUseCase usuarios = ivUser -> "ana".equals(ivUser) ? Optional.of(7) : Optional.empty();
    private final ElementosService servicio = new ElementosService(escritura, usuarios);

    private static class EscrituraEnMemoria implements ElementosWritePort {
        Integer duenoRecibido;
        CambiosElementos cambiosRecibidos;
        boolean sePuedeEditar = true;
        CapaImportada importada;

        @Override
        public Optional<List<Integer>> guardar(int capaId, int duenoId, CambiosElementos cambios) {
            duenoRecibido = duenoId;
            cambiosRecibidos = cambios;
            return sePuedeEditar ? Optional.of(List.of(10)) : Optional.empty();
        }

        @Override
        public int importar(int duenoId, CapaImportada capa) {
            duenoRecibido = duenoId;
            importada = capa;
            return 50;
        }
    }

    private static ElementoCapa elemento(int id, Geometria geometria, double radio, String nombre, EstiloElemento estilo,
                                         List<Metadato> metadatos) {
        return new ElementoCapa(id, geometria, radio, nombre, "", estilo, metadatos);
    }

    private static ElementoCapa punto() {
        return elemento(-1, PUNTO, 0, "", EstiloElemento.SIN_ESTILO, List.of());
    }

    private static CambiosElementos nuevo(ElementoCapa elemento) {
        return new CambiosElementos(List.of(elemento), List.of(), List.of());
    }

    private void exigirRechazo(ElementoCapa elemento) {
        assertThatThrownBy(() -> servicio.guardar(5, "ana", nuevo(elemento))).isInstanceOf(SolicitudInvalidaException.class);
        assertThat(escritura.cambiosRecibidos).isNull();
    }

    @Test
    void elDuenoSaleDeWebSealYEntregaLosIdDeLosNuevos() {
        assertThat(servicio.guardar(5, "ana", nuevo(punto()))).containsExactly(10);

        assertThat(escritura.duenoRecibido).isEqualTo(7);
    }

    @Test
    void unUsuarioSinRegistroNoSeDistingueDeUnaCapaAjena() {
        assertThatThrownBy(() -> servicio.guardar(5, "otro", nuevo(punto()))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unaCapaQueNoSePuedeEditarResponde404() {
        escritura.sePuedeEditar = false;

        assertThatThrownBy(() -> servicio.guardar(5, "ana", nuevo(punto()))).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void sinCambiosNoEscribeNada() {
        assertThat(servicio.guardar(5, "ana", new CambiosElementos(null, null, null))).isEmpty();

        assertThat(escritura.cambiosRecibidos).isNull();
    }

    @Test
    void rechazaGeometriasQueNoSeDibujan() {
        exigirRechazo(elemento(-1, null, 0, "", null, null));
        exigirRechazo(elemento(-1, Geometria.simple("Hexagono", List.of()), 0, "", null, null));
        exigirRechazo(elemento(-1, Geometria.coleccion(List.of(PUNTO)), 0, "", null, null));
    }

    @Test
    void elRadioSoloCorrespondeAUnPunto() {
        exigirRechazo(elemento(-1, PUNTO, -1, "", null, null));
        exigirRechazo(elemento(-1, Geometria.simple("LineString", List.of()), 5, "", null, null));
    }

    @Test
    void rechazaUnIconoDesmedido() {
        exigirRechazo(elemento(-1, PUNTO, 0, "", new EstiloElemento("", 0, "", "a".repeat(1_000_001)), null));
    }

    @Test
    void rechazaColoresYGrosoresInvalidos() {
        exigirRechazo(elemento(-1, PUNTO, 0, "", new EstiloElemento("url(http://x)", 0, "", ""), null));
        exigirRechazo(elemento(-1, PUNTO, 0, "", new EstiloElemento("", 0, "<script>", ""), null));
        exigirRechazo(elemento(-1, PUNTO, 0, "", new EstiloElemento("", -1, "", ""), null));
        exigirRechazo(elemento(-1, PUNTO, 0, "", new EstiloElemento("", 1001, "", ""), null));
    }

    @Test
    void aceptaLosColoresQueGuardaSiate() {
        for (String color : List.of("#71aecb", "#FFF", "rgba(230, 248, 253, 0.35)", "rgb(1,2,3)", "red", "")) {
            assertThat(servicio.guardar(5, "ana", nuevo(elemento(-1, PUNTO, 0, "", new EstiloElemento(color, 2, color, ""), null))))
                    .containsExactly(10);
        }
    }

    @Test
    void rechazaTextosDemasiadoLargos() {
        exigirRechazo(elemento(-1, PUNTO, 0, "x".repeat(501), null, null));
        exigirRechazo(new ElementoCapa(-1, PUNTO, 0, "", "x".repeat(65537), null, null));
        exigirRechazo(elemento(-1, PUNTO, 0, "", null, List.of(new Metadato("x".repeat(501), ""))));
        exigirRechazo(elemento(-1, PUNTO, 0, "", null, List.of(new Metadato("Fuente", "x".repeat(65537)))));
    }

    @Test
    void rechazaDemasiadosCambiosOMetadatos() {
        List<ElementoCapa> demasiados = java.util.Collections.nCopies(1001, punto());
        assertThatThrownBy(() -> servicio.guardar(5, "ana", new CambiosElementos(demasiados, List.of(), List.of())))
                .isInstanceOf(SolicitudInvalidaException.class);
        exigirRechazo(elemento(-1, PUNTO, 0, "", null, java.util.Collections.nCopies(1001, new Metadato("a", "b"))));
    }

    private static CapaImportada importada(String nombre, OrigenArchivo origen, List<ElementoCapa> elementos) {
        return new CapaImportada(nombre, "  Región ", origen, elementos);
    }

    @Test
    void importarUsaElDuenoDeWebSealYEntregaUnaCapaPrivadaSinCategoria() {
        CapaPropia capa = servicio.importar("ana", importada("  Bodegas ", OrigenArchivo.KML, List.of(punto())));

        assertThat(capa).isEqualTo(new CapaPropia(50, "Bodegas", "Región", Visibilidad.PRIVADA, 0));
        assertThat(escritura.duenoRecibido).isEqualTo(7);
        assertThat(escritura.importada.nombre()).isEqualTo("Bodegas");
        assertThat(escritura.importada.descripcion()).isEqualTo("Región");
    }

    @Test
    void importarExigeLoMismoQueCrearUnaCapa() {
        assertThatThrownBy(() -> servicio.importar("ana", importada("abc", OrigenArchivo.KML, List.of(punto()))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("al menos 4");
        assertThat(escritura.importada).isNull();
    }

    @Test
    void importarExigeUnOrigenYAlgunElemento() {
        assertThatThrownBy(() -> servicio.importar("ana", importada("Bodegas", null, List.of(punto()))))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.importar("ana", importada("Bodegas", OrigenArchivo.KML, null)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("geometrías");
    }

    @Test
    void importarLimitaLaCantidadYValidaCadaElemento() {
        List<ElementoCapa> demasiados = java.util.Collections.nCopies(10001, punto());
        assertThatThrownBy(() -> servicio.importar("ana", importada("Bodegas", OrigenArchivo.KML, demasiados)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("10000");
        assertThatThrownBy(() -> servicio.importar("ana", importada("Bodegas", OrigenArchivo.KML,
                List.of(elemento(-1, Geometria.simple("Hexagono", List.of()), 0, "", null, null)))))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThat(escritura.importada).isNull();
    }

    @Test
    void importarAceptaLaDescripcionLargaDeUnKml() {
        ElementoCapa largo = new ElementoCapa(-1, PUNTO, 0, "", "x".repeat(65536), null, null);

        assertThat(servicio.importar("ana", importada("Bodegas", OrigenArchivo.KML, List.of(largo))).id()).isEqualTo(50);
    }

    @Test
    void importarSinUsuarioRegistradoResponde404() {
        assertThatThrownBy(() -> servicio.importar("otro", importada("Bodegas", OrigenArchivo.KML, List.of(punto()))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void rechazaIdentificadoresInvalidos() {
        assertThatThrownBy(() -> servicio.guardar(5, "ana", new CambiosElementos(List.of(), List.of(punto()), List.of())))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.guardar(5, "ana", new CambiosElementos(List.of(), List.of(), List.of(-3))))
                .isInstanceOf(SolicitudInvalidaException.class);
    }
}
