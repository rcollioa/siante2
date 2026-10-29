package siante2.capas.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.application.port.out.ServiciosMapaPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.TipoServicio;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapasServiceTest {

    private static final Map<String, Integer> USUARIOS = Map.of("jperez", 7);

    private static final String WMS_IDE = "https://ide.cl/wms?request=GetCapabilities";

    /** Doble en memoria: solo el usuario 7 tiene capas y ve la 1 (vectorial), la 20 (WMS) y la 21 (WFS). */
    private static class CapasEnMemoria implements CapasReadPort {

        @Override
        public List<ResumenCapa> listarDe(int usuarioId) {
            return usuarioId != 7 ? List.of() : List.of(resumen(1, "zona norte"), resumen(2, "Árboles"),
                    resumen(3, "Arroyos"), resumen(4, "Bodegas"));
        }

        @Override
        public Optional<Capa> buscarVisible(int capaId, int usuarioId) {
            if (usuarioId != 7) {
                return Optional.empty();
            }
            return switch (capaId) {
                case 1 -> Optional.of(new Capa(1, "Bodegas", "", Visibilidad.PRIVADA, true, TipoCapa.VECTORIAL, List.of(), null));
                case 20, 21 -> Optional.of(new Capa(capaId, "IDE", "", Visibilidad.PUBLICA, false, TipoCapa.URL, List.of(), null));
                default -> Optional.empty();
            };
        }

        @Override
        public Optional<OrigenServicio> servicioVisible(int capaId, int usuarioId) {
            if (usuarioId != 7) {
                return Optional.empty();
            }
            return switch (capaId) {
                case 20 -> Optional.of(new OrigenServicio(TipoServicio.WMS, WMS_IDE));
                case 21 -> Optional.of(new OrigenServicio(TipoServicio.WFS, "https://ide.cl/wfs"));
                default -> Optional.empty();
            };
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

    /** El servidor WMS de la capa 20 publica "rios"; registra a qué URL se le pidió cada imagen. */
    private static class ServiciosEnMemoria implements ServiciosMapaPort {

        final List<String> pedidas = new ArrayList<>();

        @Override
        public List<CapaDeServicio> capasWms(String url) {
            return List.of(new CapaDeServicio("rios", "Ríos", null));
        }

        @Override
        public ImagenMapa mapaWms(String url, PedidoMapa pedido) {
            pedidas.add(url);
            return new ImagenMapa(new byte[] {1}, "image/png");
        }
    }

    private final ServiciosEnMemoria servicios = new ServiciosEnMemoria();
    private final CapasService servicio = new CapasService(new CapasEnMemoria(),
            ivUser -> Optional.ofNullable(USUARIOS.get(ivUser)), servicios);

    private static PedidoMapa pedido(String... capas) {
        return new PedidoMapa(List.of(capas), "0,0,10,10", 256, 256, "EPSG:3857", "image/png", true, "1.3.0");
    }

    @Test
    void unaCapaWmsSeAbreConLasCapasQuePublicaSuServidor() {
        Capa capa = servicio.ver(20, "jperez");

        assertThat(capa.servicio().tipo()).isEqualTo(TipoServicio.WMS);
        assertThat(capa.servicio().capas()).extracting(CapaDeServicio::nombre).containsExactly("rios");
        assertThat(servicio.ver(1, "jperez").servicio()).isNull();
    }

    @Test
    void unaCapaWfsSeAbreSinCapasHastaElProximoPaso() {
        assertThat(servicio.ver(21, "jperez").servicio().capas()).isEmpty();
    }

    @Test
    void laImagenSePideAlServidorDeLaCapaYNoAOtro() {
        assertThat(servicio.obtenerMapa(20, "jperez", pedido("rios")).tipo()).isEqualTo("image/png");
        assertThat(servicios.pedidas).containsExactly(WMS_IDE);
    }

    @Test
    void noSePidenCapasQueElServidorNoPublica() {
        assertThatThrownBy(() -> servicio.obtenerMapa(20, "jperez", pedido("rios", "secreta")))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThat(servicios.pedidas).isEmpty();
    }

    @Test
    void sinCapaWmsVisibleNoHayImagen() {
        assertThatThrownBy(() -> servicio.obtenerMapa(21, "jperez", pedido("rios"))).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.obtenerMapa(1, "jperez", pedido("rios"))).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.obtenerMapa(20, "nuevo", pedido("rios"))).isInstanceOf(RecursoNoEncontradoException.class);
    }

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
