package siante2.capas.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.capas.application.port.in.CambiarVisibilidadCapaUseCase;
import siante2.capas.application.port.in.ConsultarPuntoUseCase;
import siante2.capas.application.port.in.CrearCapaPorUrlUseCase;
import siante2.capas.application.port.in.CrearCapaUseCase;
import siante2.capas.application.port.in.EditarCapaUseCase;
import siante2.capas.application.port.in.EliminarCapaUseCase;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.ObtenerMapaUseCase;
import siante2.capas.application.port.in.ObtenerMosaicoUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.CapaPorUrl;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.EdicionCapa;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.Extension;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.PedidoMosaico;
import siante2.capas.domain.ServicioMapa;
import siante2.capas.domain.TipoServicio;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.NuevaCapa;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CapasController.class)
@Import(UsuarioDePrueba.class)
class CapasControllerTest {

    private static final String RUTA = "/siante2/api/capas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrearCapaUseCase crear;

    @Test
    void crearResponde201ConLaCapaYElDuenoSaleDeWebSeal() throws Exception {
        when(crear.crear("prueba", new NuevaCapa("Bodegas", "Región")))
                .thenReturn(new CapaPropia(50, "Bodegas", "Región", Visibilidad.PRIVADA, 0));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Bodegas","descripcion":"Región","dueno":"otro","owner":9}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.nombre").value("Bodegas"))
                .andExpect(jsonPath("$.visibilidad").value("PRIVADA"))
                .andExpect(jsonPath("$.categoriaId").value(0));
    }

    @Test
    void crearConDatosInvalidosResponde400ConElMensaje() throws Exception {
        when(crear.crear("prueba", new NuevaCapa("", ""))).thenThrow(new SolicitudInvalidaException("Nombre requerido."));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"","descripcion":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nombre requerido."));
    }

    @MockitoBean
    private CrearCapaPorUrlUseCase crearPorUrl;

    @Test
    void crearPorUrlResponde201ConLaCapaYElDuenoSaleDeWebSeal() throws Exception {
        when(crearPorUrl.crearPorUrl("prueba", new CapaPorUrl("Ríos", "Región", "https://ide.cl/wms")))
                .thenReturn(new CapaPropia(50, "Ríos", "Región", Visibilidad.PRIVADA, 0));

        mockMvc.perform(post(RUTA + "/url").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ríos","descripcion":"Región","url":"https://ide.cl/wms","owner":9}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.url").doesNotExist());
    }

    @Test
    void crearPorUrlInvalidaResponde400ConElMensaje() throws Exception {
        when(crearPorUrl.crearPorUrl("prueba", new CapaPorUrl("Ríos", "", "x")))
                .thenThrow(new SolicitudInvalidaException("Debe ser una URL válida."));

        mockMvc.perform(post(RUTA + "/url").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ríos","descripcion":"","url":"x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe ser una URL válida."));
    }

    @MockitoBean
    private EditarCapaUseCase editar;

    @MockitoBean
    private CambiarVisibilidadCapaUseCase visibilidad;

    @MockitoBean
    private EliminarCapaUseCase eliminar;

    @Test
    void editarResponde204YElUsuarioSaleDeWebSeal() throws Exception {
        mockMvc.perform(patch(RUTA + "/7").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Bodegas","descripcion":"Región","categoriaId":3,"visibilidad":"PUBLICA"}"""))
                .andExpect(status().isNoContent());

        verify(editar).editar(7, "prueba", new EdicionCapa("Bodegas", "Región", 3, Visibilidad.PUBLICA));
    }

    @Test
    void elCuerpoNoPuedeElegirAlDueno() throws Exception {
        mockMvc.perform(patch(RUTA + "/7").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Bodegas","descripcion":"","categoriaId":3,"visibilidad":"PRIVADA","dueno":"otro","owner":9}"""))
                .andExpect(status().isNoContent());

        verify(editar).editar(7, "prueba", new EdicionCapa("Bodegas", "", 3, Visibilidad.PRIVADA));
    }

    @Test
    void datosInvalidosResponden400ConElMensaje() throws Exception {
        doThrow(new SolicitudInvalidaException("Nombre requerido.")).when(editar).editar(eq(7), eq("prueba"), argThat(e -> e.nombre().isEmpty()));

        mockMvc.perform(patch(RUTA + "/7").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"","descripcion":"","categoriaId":3,"visibilidad":"PRIVADA"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Nombre requerido."));
    }

    @Test
    void unaVisibilidadQueNoExisteResponde400SinLlegarAlCasoDeUso() throws Exception {
        mockMvc.perform(patch(RUTA + "/7/visibilidad").contentType(MediaType.APPLICATION_JSON).content("""
                        {"visibilidad":"OTRA"}"""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(visibilidad);
    }

    @Test
    void publicarResponde204() throws Exception {
        mockMvc.perform(patch(RUTA + "/7/visibilidad").contentType(MediaType.APPLICATION_JSON).content("""
                        {"visibilidad":"PUBLICA"}"""))
                .andExpect(status().isNoContent());

        verify(visibilidad).cambiarVisibilidad(7, "prueba", Visibilidad.PUBLICA);
    }

    @Test
    void eliminarResponde204() throws Exception {
        mockMvc.perform(post(RUTA + "/7/eliminar")).andExpect(status().isNoContent());

        verify(eliminar).eliminar(7, "prueba");
    }

    @Test
    void escribirEnUnaCapaAjenaResponde404() throws Exception {
        doThrow(new RecursoNoEncontradoException("La capa no existe.")).when(eliminar).eliminar(8, "prueba");

        mockMvc.perform(post(RUTA + "/8/eliminar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @MockitoBean
    private ListarMisCapasUseCase listar;

    @MockitoBean
    private VerCapaUseCase ver;

    @MockitoBean
    private ListarFavoritasUseCase favoritas;

    @MockitoBean
    private VerCatalogoUseCase catalogo;

    @MockitoBean
    private ObtenerMapaUseCase mapa;

    @MockitoBean
    private ConsultarPuntoUseCase consulta;

    @MockitoBean
    private ObtenerMosaicoUseCase mosaico;

    @Test
    void unaCapaWmsTraeLasCapasDeSuServidorSinSuUrl() throws Exception {
        when(ver.ver(7, "prueba")).thenReturn(new Capa(7, "IDE", "", Visibilidad.PUBLICA, false, false, TipoCapa.URL, List.of(),
                new ServicioMapa(TipoServicio.WMS, List.of(new CapaDeServicio("rios", "Ríos", new Extension(-76, -56, -66, -17))))));

        mockMvc.perform(get(RUTA + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servicio.tipo").value("WMS"))
                .andExpect(jsonPath("$.servicio.capas[0].nombre").value("rios"))
                .andExpect(jsonPath("$.servicio.capas[0].extension.oeste").value(-76))
                .andExpect(jsonPath("$.servicio.disponible").value(true))
                .andExpect(jsonPath("$.servicio.url").doesNotExist());
    }

    @Test
    void entregaLaImagenWmsConSuTipoYSeGuardaUnosMinutosEnElNavegador() throws Exception {
        when(mapa.obtenerMapa(eq(7), eq("prueba"), argThat((PedidoMapa p) -> p.capas().equals(List.of("rios", "lagos"))
                && p.crs().equals("EPSG:3857") && p.ancho() == 256))).thenReturn(new ImagenMapa(new byte[] {1, 2}, "image/png"));

        mockMvc.perform(get(RUTA + "/7/wms").param("SERVICE", "WMS").param("REQUEST", "GetMap").param("VERSION", "1.3.0")
                        .param("LAYERS", "rios,lagos").param("CRS", "EPSG:3857").param("BBOX", "-7.9e6,-4e6,-7.8e6,-3.9e6")
                        .param("WIDTH", "256").param("HEIGHT", "256").param("FORMAT", "image/png").param("TRANSPARENT", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", "max-age=600, private"))
                .andExpect(content().bytes(new byte[] {1, 2}));
    }

    @Test
    void unaImagenWmsConParametrosInvalidosResponde400SinPreguntarAlServidor() throws Exception {
        mockMvc.perform(get(RUTA + "/7/wms").param("LAYERS", "rios").param("CRS", "EPSG:3857").param("BBOX", "1,2,3")
                        .param("WIDTH", "256").param("HEIGHT", "256"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get(RUTA + "/7/wms").param("LAYERS", "rios").param("CRS", "EPSG:3857").param("BBOX", "1,2,3,4")
                        .param("WIDTH", "99999").param("HEIGHT", "256"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(mapa);
    }

    @Test
    void entregaElMosaicoWmtsConSuTipoYSeGuardaUnosMinutosEnElNavegador() throws Exception {
        when(mosaico.obtenerMosaico(7, "prueba", new PedidoMosaico("ide:orto", "EPSG:3857:5", 10, 20)))
                .thenReturn(new ImagenMapa(new byte[] {3}, "image/jpeg"));

        mockMvc.perform(get(RUTA + "/7/wmts/ide:orto/EPSG:3857:5/10/20"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", "max-age=600, private"))
                .andExpect(content().bytes(new byte[] {3}));
    }

    @Test
    void unMosaicoConFilaNegativaResponde400SinPreguntarAlServidor() throws Exception {
        mockMvc.perform(get(RUTA + "/7/wmts/orto/0/-1/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(mosaico);
    }

    @Test
    void laConsultaDeUnPuntoEntregaLosElementosDelServidor() throws Exception {
        when(consulta.consultarPunto(eq(7), eq("prueba"), argThat((ConsultaPunto c) -> c.i() == 10 && c.j() == 20
                && c.mapa().capas().equals(List.of("rios")) && c.mapa().version().equals("1.1.1"))))
                .thenReturn(List.of(new ElementoDeServicio("Ríos", List.of(new Metadato("NOMBRE", "Maipo")))));

        mockMvc.perform(get(RUTA + "/7/wms/info").param("LAYERS", "rios").param("CRS", "EPSG:3857")
                        .param("BBOX", "1,2,3,4").param("WIDTH", "256").param("HEIGHT", "256").param("VERSION", "1.1.1")
                        .param("I", "10").param("J", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].capa").value("Ríos"))
                .andExpect(jsonPath("$.data[0].datos[0].nombre").value("NOMBRE"))
                .andExpect(jsonPath("$.data[0].datos[0].valor").value("Maipo"));
    }

    @Test
    void unPuntoFueraDeLaImagenResponde400SinPreguntarAlServidor() throws Exception {
        mockMvc.perform(get(RUTA + "/7/wms/info").param("LAYERS", "rios").param("CRS", "EPSG:3857")
                        .param("BBOX", "1,2,3,4").param("WIDTH", "256").param("HEIGHT", "256")
                        .param("I", "256").param("J", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(consulta);
    }

    @Test
    void listaLasFavoritasDelUsuarioDeWebSeal() throws Exception {
        when(favoritas.listarFavoritas("prueba")).thenReturn(List.of(new ResumenCapa(3, "Hospitales", "", Visibilidad.FORMAL)));

        mockMvc.perform(get(RUTA + "/favoritas"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", startsWith("no-cache")))
                .andExpect(jsonPath("$.data[0].nombre").value("Hospitales"))
                .andExpect(jsonPath("$.data[0].visibilidad").value("FORMAL"));
    }

    @Test
    void entregaUnaPestanaDelCatalogo() throws Exception {
        when(catalogo.verCatalogo(SeccionCatalogo.FORMALES, "prueba")).thenReturn(List.of(
                new CapaCatalogo(3, "Hospitales", "MINSAL", Visibilidad.FORMAL, "Salud", "SIANTE", true)));

        mockMvc.perform(get(RUTA + "/catalogo").param("seccion", "FORMALES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoria").value("Salud"))
                .andExpect(jsonPath("$.data[0].dueno").value("SIANTE"))
                .andExpect(jsonPath("$.data[0].favorita").value(true));
    }

    @Test
    void unaPestanaDesconocidaDelCatalogoResponde400() throws Exception {
        mockMvc.perform(get(RUTA + "/catalogo").param("seccion", "TODAS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void listaLasCapasDelUsuarioDeWebSealYNoSeGuardanEnElNavegador() throws Exception {
        when(listar.listar("prueba")).thenReturn(List.of(new CapaPropia(1, "Bodegas", "Región", Visibilidad.PRIVADA, 9)));

        mockMvc.perform(get(RUTA + "/mias"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", startsWith("no-cache")))
                .andExpect(jsonPath("$.data[0].nombre").value("Bodegas"))
                .andExpect(jsonPath("$.data[0].visibilidad").value("PRIVADA"))
                .andExpect(jsonPath("$.data[0].categoriaId").value(9));
    }

    @Test
    void entregaUnaCapaConSusElementosEnGeoJson() throws Exception {
        ElementoCapa circulo = new ElementoCapa(0, Geometria.simple("Point", List.of(-70.65, -33.44)), 500, "Radio",
                "", new EstiloElemento("#3399CC", 2, "", ""), List.of(new Metadato("Fuente", "Terreno")));
        when(ver.ver(5, "prueba")).thenReturn(
                new Capa(5, "Bodegas", "", Visibilidad.PUBLICA, true, true, TipoCapa.VECTORIAL, List.of(circulo), null));

        mockMvc.perform(get(RUTA + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propia").value(true))
                .andExpect(jsonPath("$.tipo").value("VECTORIAL"))
                .andExpect(jsonPath("$.elementos[0].geometria.type").value("Point"))
                .andExpect(jsonPath("$.elementos[0].geometria.geometries").doesNotExist())
                .andExpect(jsonPath("$.elementos[0].radio").value(500))
                .andExpect(jsonPath("$.elementos[0].estilo.color").value("#3399CC"))
                .andExpect(jsonPath("$.elementos[0].metadatos[0].valor").value("Terreno"));
    }

    @Test
    void unaCapaQueNoPuedeVerResponde404() throws Exception {
        when(ver.ver(9, "prueba")).thenThrow(new RecursoNoEncontradoException("La capa no existe."));

        mockMvc.perform(get(RUTA + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La capa no existe."));
    }
}
