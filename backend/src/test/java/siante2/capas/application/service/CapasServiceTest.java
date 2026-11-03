package siante2.capas.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.application.port.out.CapasWritePort;
import siante2.capas.application.port.out.ServiciosMapaPort;
import siante2.capas.application.port.out.ServiciosWmtsPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.EdicionCapa;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.GrillaWmts;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.MatrizMosaicos;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.NuevaCapa;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.PedidoMosaico;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.ServicioMapa;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.TipoServicio;
import siante2.capas.domain.Visibilidad;
import siante2.categorias.domain.Categoria;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.compartido.domain.SolicitudInvalidaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapasServiceTest {

    private static final Map<String, Integer> USUARIOS = Map.of("jperez", 7);

    private static final String WMS_IDE = "https://ide.cl/wms?request=GetCapabilities";
    private static final String WMS_CAIDO = "https://caido.cl/wms?request=GetCapabilities";

    /** Doble en memoria: solo el usuario 7 tiene capas y ve la 1 (vectorial), la 20 (WMS), la 21 (WFS) y la 22 (WMS
     * de un servidor caído). */
    private static class CapasEnMemoria implements CapasReadPort {

        @Override
        public List<CapaPropia> listarDe(int usuarioId) {
            return usuarioId != 7 ? List.of() : List.of(propia(1, "zona norte"), propia(2, "Árboles"),
                    propia(3, "Arroyos"), propia(4, "Bodegas"));
        }

        private static CapaPropia propia(int id, String nombre) {
            return new CapaPropia(id, nombre, "", Visibilidad.PRIVADA, 3);
        }

        @Override
        public Optional<Capa> buscarVisible(int capaId, int usuarioId) {
            if (usuarioId != 7) {
                return Optional.empty();
            }
            return switch (capaId) {
                case 1 -> Optional.of(new Capa(1, "Bodegas", "", Visibilidad.PRIVADA, true, true, TipoCapa.VECTORIAL, List.of(), null));
                case 20, 21, 22, 23 -> Optional.of(new Capa(capaId, "IDE", "", Visibilidad.PUBLICA, false, false, TipoCapa.URL, List.of(), null));
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
                case 22 -> Optional.of(new OrigenServicio(TipoServicio.WMS, WMS_CAIDO));
                case 23 -> Optional.of(new OrigenServicio(TipoServicio.WMTS, "https://wmts.cl/WMTSCapabilities.xml"));
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
            if (url.equals(WMS_CAIDO)) {
                throw new ServicioExternoNoDisponibleException("El servidor de mapas de la capa no responde.");
            }
            return List.of(new CapaDeServicio("rios", "Ríos", null));
        }

        @Override
        public ImagenMapa mapaWms(String url, PedidoMapa pedido) {
            pedidas.add(url);
            return new ImagenMapa(new byte[] {1}, "image/png");
        }

        /** Un elemento de "rios" y otro de una capa que el servidor no informa en su GetCapabilities. */
        @Override
        public List<ElementoDeServicio> infoWms(String url, ConsultaPunto consulta) {
            pedidas.add(url);
            return List.of(new ElementoDeServicio("rios", List.of(new Metadato("NOMBRE", "Maipo"))),
                    new ElementoDeServicio("otra", List.of()));
        }
    }

    /** El servidor WMTS de la capa 23 publica "orto" con dos niveles: el 0 de 1×1 mosaicos y el 1 de 2×2. */
    private static class MosaicosEnMemoria implements ServiciosWmtsPort {

        final List<PedidoMosaico> pedidos = new ArrayList<>();

        @Override
        public ServicioMapa servicioWmts(String url) {
            return ServicioMapa.wmts(List.of(new CapaDeServicio("orto", "Ortofoto", null)), new GrillaWmts("EPSG:3857",
                    List.of(new MatrizMosaicos("0", 156543.03, -2.0e7, 2.0e7, 256, 256, 1, 1),
                            new MatrizMosaicos("1", 78271.52, -2.0e7, 2.0e7, 256, 256, 2, 2))));
        }

        @Override
        public ImagenMapa mosaicoWmts(String url, PedidoMosaico pedido) {
            pedidos.add(pedido);
            return new ImagenMapa(new byte[] {2}, "image/jpeg");
        }
    }

    /** Solo el usuario 7 es dueño, y solo de la capa 1; registra lo que se escribió. */
    private static class EscrituraEnMemoria implements CapasWritePort {

        EdicionCapa editada;
        Visibilidad visibilidad;
        boolean eliminada;
        NuevaCapa creada;
        int duenoDeLaCreada;

        private static boolean esDelDueno(int capaId, int duenoId) {
            return capaId == 1 && duenoId == 7;
        }

        @Override
        public int crear(int duenoId, NuevaCapa nueva) {
            creada = nueva;
            duenoDeLaCreada = duenoId;
            return 50;
        }

        @Override
        public boolean actualizar(int capaId, int duenoId, EdicionCapa edicion) {
            editada = esDelDueno(capaId, duenoId) ? edicion : null;
            return editada != null;
        }

        @Override
        public boolean cambiarVisibilidad(int capaId, int duenoId, Visibilidad nueva) {
            visibilidad = esDelDueno(capaId, duenoId) ? nueva : null;
            return visibilidad != null;
        }

        @Override
        public boolean eliminar(int capaId, int duenoId) {
            eliminada = esDelDueno(capaId, duenoId);
            return eliminada;
        }
    }

    private final ServiciosEnMemoria servicios = new ServiciosEnMemoria();
    private final MosaicosEnMemoria mosaicos = new MosaicosEnMemoria();
    private final EscrituraEnMemoria escritura = new EscrituraEnMemoria();
    private final CapasService servicio = new CapasService(new CapasEnMemoria(), escritura,
            ivUser -> Optional.ofNullable(USUARIOS.get(ivUser)), () -> List.of(new Categoria(3, "Agua", "")), servicios, mosaicos);

    private static EdicionCapa edicion(String nombre, String descripcion) {
        return new EdicionCapa(nombre, descripcion, 3, Visibilidad.PUBLICA);
    }

    @Test
    void crearGuardaAlUsuarioDeWebSealComoDuenoYEntregaLaCapaPrivadaYSinCategoria() {
        CapaPropia capa = servicio.crear("jperez", new NuevaCapa("  Bodegas norte ", " Región "));

        assertThat(escritura.duenoDeLaCreada).isEqualTo(7);
        assertThat(escritura.creada).isEqualTo(new NuevaCapa("Bodegas norte", "Región"));
        assertThat(capa).isEqualTo(new CapaPropia(50, "Bodegas norte", "Región", Visibilidad.PRIVADA, 0));
    }

    @Test
    void crearSinDescripcionLaGuardaVacia() {
        assertThat(servicio.crear("jperez", new NuevaCapa("Bodegas", null)).descripcion()).isEmpty();
    }

    @Test
    void crearExigeLosMismosLimitesQueEditarSinEscribir() {
        assertThatThrownBy(() -> servicio.crear("jperez", new NuevaCapa(null, "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Nombre requerido.");
        assertThatThrownBy(() -> servicio.crear("jperez", new NuevaCapa(" abc ", "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El nombre debe tener al menos 4 caracteres.");
        assertThatThrownBy(() -> servicio.crear("jperez", new NuevaCapa("a".repeat(101), "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El nombre no puede tener más de 100 caracteres.");
        assertThatThrownBy(() -> servicio.crear("jperez", new NuevaCapa("Bodegas", "a".repeat(201))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("La descripción no puede tener más de 200 caracteres.");
        assertThat(escritura.creada).isNull();
    }

    @Test
    void unUsuarioSinRegistroNoPuedeCrear() {
        assertThatThrownBy(() -> servicio.crear("nuevo", new NuevaCapa("Bodegas", "")))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El usuario no está registrado.");
        assertThat(escritura.creada).isNull();
    }

    @Test
    void editarGuardaLosDatosSinEspaciosALosLados() {
        servicio.editar(1, "jperez", new EdicionCapa("  Bodegas norte ", " Región ", 3, Visibilidad.PUBLICA));

        assertThat(escritura.editada).isEqualTo(new EdicionCapa("Bodegas norte", "Región", 3, Visibilidad.PUBLICA));
    }

    @Test
    void sinDescripcionSeGuardaVacia() {
        servicio.editar(1, "jperez", edicion("Bodegas", null));

        assertThat(escritura.editada.descripcion()).isEmpty();
    }

    @Test
    void rechazaUnNombreSinLosLimitesDeSiate() {
        assertThatThrownBy(() -> servicio.editar(1, "jperez", edicion(null, "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Nombre requerido.");
        assertThatThrownBy(() -> servicio.editar(1, "jperez", edicion("     ", "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Nombre requerido.");
        assertThatThrownBy(() -> servicio.editar(1, "jperez", edicion(" abc ", "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El nombre debe tener al menos 4 caracteres.");
        assertThatThrownBy(() -> servicio.editar(1, "jperez", edicion("a".repeat(101), "")))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El nombre no puede tener más de 100 caracteres.");
        assertThatThrownBy(() -> servicio.editar(1, "jperez", edicion("Bodegas", "a".repeat(201))))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("La descripción no puede tener más de 200 caracteres.");
        assertThat(escritura.editada).isNull();
    }

    @Test
    void aceptaLosLimitesExactos() {
        servicio.editar(1, "jperez", edicion("abcd", "a".repeat(200)));
        assertThat(escritura.editada.nombre()).isEqualTo("abcd");

        servicio.editar(1, "jperez", edicion("a".repeat(100), ""));
        assertThat(escritura.editada.nombre()).hasSize(100);
    }

    @Test
    void rechazaUnaCategoriaQueNoExisteSinEscribir() {
        assertThatThrownBy(() -> servicio.editar(1, "jperez", new EdicionCapa("Bodegas", "", 99, Visibilidad.PRIVADA)))
                .isInstanceOf(SolicitudInvalidaException.class).hasMessage("La categoría no existe.");
        assertThat(escritura.editada).isNull();
    }

    @Test
    void unaCapaNoSePuedeVolverFormalNiAreaDeDibujo() {
        assertThatThrownBy(() -> servicio.editar(1, "jperez", new EdicionCapa("Bodegas", "", 3, Visibilidad.FORMAL)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.editar(1, "jperez", new EdicionCapa("Bodegas", "", 3, null)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.cambiarVisibilidad(1, "jperez", Visibilidad.AREA_DE_DIBUJO))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.cambiarVisibilidad(1, "jperez", null))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThat(escritura.editada).isNull();
        assertThat(escritura.visibilidad).isNull();
    }

    @Test
    void publicarYEliminarAplicanSobreLaCapaPropia() {
        servicio.cambiarVisibilidad(1, "jperez", Visibilidad.PUBLICA);
        assertThat(escritura.visibilidad).isEqualTo(Visibilidad.PUBLICA);

        servicio.eliminar(1, "jperez");
        assertThat(escritura.eliminada).isTrue();
    }

    @Test
    void escribirEnUnaCapaAjenaOSinRegistroEsLoMismoQueNoEncontrarla() {
        assertThatThrownBy(() -> servicio.editar(2, "jperez", edicion("Bodegas", "")))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("La capa no existe.");
        assertThatThrownBy(() -> servicio.cambiarVisibilidad(2, "jperez", Visibilidad.PUBLICA))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.eliminar(2, "jperez")).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.editar(1, "nuevo", edicion("Bodegas", "")))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.eliminar(1, "nuevo")).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unaCapaWmtsSeAbreConSusCapasYLaGrillaDeMosaicos() {
        Capa capa = servicio.ver(23, "jperez");

        assertThat(capa.servicio().tipo()).isEqualTo(TipoServicio.WMTS);
        assertThat(capa.servicio().capas()).extracting(CapaDeServicio::nombre).containsExactly("orto");
        assertThat(capa.servicio().grilla().matrices()).extracting(MatrizMosaicos::id).containsExactly("0", "1");
    }

    @Test
    void elMosaicoSePideSoloSiEstaEnLaGrillaDeUnaCapaPublicada() {
        assertThat(servicio.obtenerMosaico(23, "jperez", new PedidoMosaico("orto", "1", 1, 1)).tipo()).isEqualTo("image/jpeg");

        assertThatThrownBy(() -> servicio.obtenerMosaico(23, "jperez", new PedidoMosaico("secreta", "1", 0, 0)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.obtenerMosaico(23, "jperez", new PedidoMosaico("orto", "9", 0, 0)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.obtenerMosaico(23, "jperez", new PedidoMosaico("orto", "0", 1, 0)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.obtenerMosaico(20, "jperez", new PedidoMosaico("orto", "0", 0, 0)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> new PedidoMosaico("orto", "0", -1, 0)).isInstanceOf(SolicitudInvalidaException.class);
        assertThat(mosaicos.pedidos).hasSize(1);
    }

    private static PedidoMapa pedido(String... capas) {
        return new PedidoMapa(List.of(capas), "0,0,10,10", 256, 256, "EPSG:3857", "image/png", true, "1.3.0");
    }

    @Test
    void unaCapaWmsSeAbreConLasCapasQuePublicaSuServidor() {
        Capa capa = servicio.ver(20, "jperez");

        assertThat(capa.servicio().tipo()).isEqualTo(TipoServicio.WMS);
        assertThat(capa.servicio().capas()).extracting(CapaDeServicio::nombre).containsExactly("rios");
        assertThat(capa.servicio().disponible()).isTrue();
        assertThat(servicio.ver(1, "jperez").servicio()).isNull();
    }

    @Test
    void siElServidorWmsNoRespondeLaCapaSeAbreIgualSinContenidoComoEnSiate() {
        Capa capa = servicio.ver(22, "jperez");

        assertThat(capa.servicio().tipo()).isEqualTo(TipoServicio.WMS);
        assertThat(capa.servicio().capas()).isEmpty();
        assertThat(capa.servicio().disponible()).isFalse();
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
    void laConsultaDeUnPuntoVaAlServidorDeLaCapaYMuestraElTituloDeCadaCapa() {
        assertThat(servicio.consultarPunto(20, "jperez", new ConsultaPunto(pedido("rios"), 10, 20)))
                .extracting(ElementoDeServicio::capa)
                .containsExactly("Ríos", "otra");
        assertThat(servicios.pedidas).hasSize(1);
    }

    @Test
    void laConsultaDeUnPuntoTieneLasMismasReglasQueLaImagen() {
        assertThatThrownBy(() -> servicio.consultarPunto(20, "jperez", new ConsultaPunto(pedido("secreta"), 0, 0)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.consultarPunto(21, "jperez", new ConsultaPunto(pedido("rios"), 0, 0)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(servicios.pedidas).isEmpty();
    }

    @Test
    void elPuntoConsultadoDebeEstarDentroDeLaImagen() {
        assertThatThrownBy(() -> new ConsultaPunto(pedido("rios"), 256, 0)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> new ConsultaPunto(pedido("rios"), 0, -1)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> new ConsultaPunto(null, 0, 0)).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void ordenaLasCapasComoUnDiccionarioSinImportarTildesNiMayusculas() {
        assertThat(servicio.listar("jperez")).extracting(CapaPropia::nombre)
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
