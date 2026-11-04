package siante2.capas.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.capas.application.port.out.IconosPort;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IconosServiceTest {

    private static final String PNG = "data:image/png;base64,iVBORw0KGgo=";
    private static final String SVG = "data:image/svg+xml;base64,PHN2Zy8+";
    private static final Map<String, Integer> USUARIOS = Map.of("jperez", 7);

    /** Doble en memoria: el usuario 7 es dueño del ícono 5. */
    private static class IconosEnMemoria implements IconosPort {

        final List<DatosIcono> escritos = new ArrayList<>();
        int pedidoPor;
        boolean eliminado;

        @Override
        public List<Icono> listar(int usuarioId) {
            pedidoPor = usuarioId;
            return List.of(new Icono(1, "Sistema", "", PNG, false));
        }

        @Override
        public int crear(int duenoId, DatosIcono datos) {
            escritos.add(datos);
            return 50;
        }

        @Override
        public boolean actualizar(int id, int duenoId, DatosIcono datos) {
            escritos.add(datos);
            return id == 5 && duenoId == 7;
        }

        @Override
        public boolean eliminar(int id, int duenoId) {
            eliminado = id == 5 && duenoId == 7;
            return eliminado;
        }
    }

    private final IconosEnMemoria iconos = new IconosEnMemoria();
    private final IconosService servicio = new IconosService(iconos, ivUser -> Optional.ofNullable(USUARIOS.get(ivUser)));

    private static DatosIcono datos(String imagen) {
        return new DatosIcono("Bodega", "bodega.png", imagen);
    }

    @Test
    void listaLosDelUsuarioDeWebSeal() {
        assertThat(servicio.listar("jperez")).hasSize(1);
        assertThat(iconos.pedidoPor).isEqualTo(7);
    }

    @Test
    void sinRegistroSoloSeVenLosDelSistema() {
        assertThat(servicio.listar("nuevo")).hasSize(1);
        assertThat(iconos.pedidoPor).isEqualTo(-1);
    }

    @Test
    void creaSinEspaciosALosLadosYLoEntregaComoPropio() {
        Icono icono = servicio.crear("jperez", new DatosIcono(" Bodega ", null, SVG));

        assertThat(icono).isEqualTo(new Icono(50, "Bodega", "", SVG, true));
        assertThat(iconos.escritos).containsExactly(new DatosIcono("Bodega", "", SVG));
    }

    @Test
    void unUsuarioSinRegistroNoPuedeCrear() {
        assertThatThrownBy(() -> servicio.crear("nuevo", datos(PNG)))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El usuario no está registrado.");
    }

    @Test
    void exigeNombreYLargosSinEscribir() {
        assertThatThrownBy(() -> servicio.crear("jperez", new DatosIcono(null, "", PNG)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Nombre requerido.");
        assertThatThrownBy(() -> servicio.crear("jperez", new DatosIcono("   ", "", PNG)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Nombre requerido.");
        assertThatThrownBy(() -> servicio.crear("jperez", new DatosIcono("a".repeat(201), "", PNG)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El nombre no puede tener más de 200 caracteres.");
        assertThatThrownBy(() -> servicio.crear("jperez", new DatosIcono("Bodega", "a".repeat(201), PNG)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El nombre del archivo no puede tener más de 200 caracteres.");
        assertThat(iconos.escritos).isEmpty();
    }

    @Test
    void soloAceptaPngYSvgIncrustadosYBienCodificados() {
        assertThatThrownBy(() -> servicio.crear("jperez", datos(null)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("La imagen del ícono falta o es demasiado grande.");
        assertThatThrownBy(() -> servicio.crear("jperez", datos("data:image/png;base64," + "a".repeat(32000))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("La imagen del ícono falta o es demasiado grande.");
        assertThatThrownBy(() -> servicio.crear("jperez", datos("https://otro.cl/icono.png")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Los tipos de archivos permitidos son SVG y PNG.");
        assertThatThrownBy(() -> servicio.crear("jperez", datos("data:text/html;base64,PGI+")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Los tipos de archivos permitidos son SVG y PNG.");
        assertThatThrownBy(() -> servicio.crear("jperez", datos("data:image/png;base64,no es base64")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Este tipo de archivo presenta problemas de lectura.");
        assertThat(iconos.escritos).isEmpty();
    }

    @Test
    void editarUnIconoPropioValidaYEscribe() {
        servicio.editar(5, "jperez", datos(PNG));

        assertThat(iconos.escritos).containsExactly(datos(PNG));
        assertThatThrownBy(() -> servicio.editar(5, "jperez", datos("x"))).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void unIconoAjenoODeOtroUsuarioNoSeDistingueDeUnoQueNoExiste() {
        assertThatThrownBy(() -> servicio.editar(6, "jperez", datos(PNG)))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El ícono no existe.");
        assertThatThrownBy(() -> servicio.eliminar(6, "jperez"))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El ícono no existe.");
        assertThatThrownBy(() -> servicio.eliminar(5, "nuevo"))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El ícono no existe.");
    }

    @Test
    void eliminaUnIconoPropio() {
        servicio.eliminar(5, "jperez");

        assertThat(iconos.eliminado).isTrue();
    }
}
