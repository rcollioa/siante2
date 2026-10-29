package siante2.capas.application.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.domain.RecursoNoEncontradoException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapasServiceTest {

    private static final Map<String, Integer> USUARIOS = Map.of("jperez", 7);

    /** Doble en memoria: solo el usuario 7 tiene capas y solo ve la capa 1. */
    private static class CapasEnMemoria implements CapasReadPort {

        @Override
        public List<ResumenCapa> listarDe(int usuarioId) {
            return usuarioId != 7 ? List.of() : List.of(resumen(1, "zona norte"), resumen(2, "Árboles"),
                    resumen(3, "Arroyos"), resumen(4, "Bodegas"));
        }

        @Override
        public Optional<Capa> buscarVisible(int capaId, int usuarioId) {
            return capaId == 1 && usuarioId == 7
                    ? Optional.of(new Capa(1, "Bodegas", "", Visibilidad.PRIVADA, true, TipoCapa.VECTORIAL, List.of()))
                    : Optional.empty();
        }

        @Override
        public List<ResumenCapa> listarFavoritasDe(int usuarioId) {
            return usuarioId != 7 ? List.of() : List.of(resumen(5, "Salud"), resumen(6, "Agua"));
        }

        /** Las públicas las ve cualquiera; las propias, solo un usuario registrado. */
        @Override
        public List<CapaCatalogo> catalogo(SeccionCatalogo seccion, int usuarioId) {
            if (seccion == SeccionCatalogo.MIAS && usuarioId != 7) {
                return List.of();
            }
            return List.of(catalogo(8, "Ríos", usuarioId == 7), catalogo(9, "Calles", false));
        }

        private static CapaCatalogo catalogo(int id, String nombre, boolean favorita) {
            return new CapaCatalogo(id, nombre, "", Visibilidad.PUBLICA, "Agua", "Ana Pérez", favorita);
        }

        private static ResumenCapa resumen(int id, String nombre) {
            return new ResumenCapa(id, nombre, "", Visibilidad.PRIVADA);
        }
    }

    private final CapasService servicio = new CapasService(new CapasEnMemoria(),
            ivUser -> Optional.ofNullable(USUARIOS.get(ivUser)));

    @Test
    void ordenaLasCapasComoUnDiccionarioSinImportarTildesNiMayusculas() {
        assertThat(servicio.listar("jperez")).extracting(ResumenCapa::nombre)
                .containsExactly("Árboles", "Arroyos", "Bodegas", "zona norte");
    }

    @Test
    void unUsuarioQueNuncaIngresoNoTieneCapas() {
        assertThat(servicio.listar("nuevo")).isEmpty();
        assertThat(servicio.listarFavoritas("nuevo")).isEmpty();
    }

    @Test
    void lasFavoritasTambienVanPorNombre() {
        assertThat(servicio.listarFavoritas("jperez")).extracting(ResumenCapa::nombre).containsExactly("Agua", "Salud");
    }

    @Test
    void elCatalogoVaPorNombreYMarcaLasFavoritasDelUsuario() {
        assertThat(servicio.verCatalogo(SeccionCatalogo.PUBLICAS, "jperez"))
                .extracting(CapaCatalogo::nombre, CapaCatalogo::favorita)
                .containsExactly(tuple("Calles", false), tuple("Ríos", true));
    }

    @Test
    void sinRegistroElCatalogoMuestraLoPublicoPeroNadaPropio() {
        assertThat(servicio.verCatalogo(SeccionCatalogo.PUBLICAS, "nuevo")).hasSize(2).noneMatch(CapaCatalogo::favorita);
        assertThat(servicio.verCatalogo(SeccionCatalogo.MIAS, "nuevo")).isEmpty();
    }

    @Test
    void verUnaCapaAjenaOInexistenteEsLoMismoQueNoEncontrarla() {
        assertThat(servicio.ver(1, "jperez").nombre()).isEqualTo("Bodegas");
        assertThatThrownBy(() -> servicio.ver(2, "jperez"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La capa no existe.");
        assertThatThrownBy(() -> servicio.ver(1, "nuevo")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
