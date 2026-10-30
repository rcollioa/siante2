package siante2.capas.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.capas.application.port.in.ConsultarPuntoUseCase;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.ObtenerMapaUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.Extension;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.ServicioMapa;
import siante2.capas.domain.TipoServicio;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.RecursoNoEncontradoException;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void unaCapaWmsTraeLasCapasDeSuServidorSinSuUrl() throws Exception {
        when(ver.ver(7, "prueba")).thenReturn(new Capa(7, "IDE", "", Visibilidad.PUBLICA, false, TipoCapa.URL, List.of(),
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
        when(listar.listar("prueba")).thenReturn(List.of(new ResumenCapa(1, "Bodegas", "Región", Visibilidad.PRIVADA)));

        mockMvc.perform(get(RUTA + "/mias"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", startsWith("no-cache")))
                .andExpect(jsonPath("$.data[0].nombre").value("Bodegas"))
                .andExpect(jsonPath("$.data[0].visibilidad").value("PRIVADA"));
    }

    @Test
    void entregaUnaCapaConSusElementosEnGeoJson() throws Exception {
        ElementoCapa circulo = new ElementoCapa(0, Geometria.simple("Point", List.of(-70.65, -33.44)), 500, "Radio",
                "", new EstiloElemento("#3399CC", 2, "", ""), List.of(new Metadato("Fuente", "Terreno")));
        when(ver.ver(5, "prueba")).thenReturn(
                new Capa(5, "Bodegas", "", Visibilidad.PUBLICA, true, TipoCapa.VECTORIAL, List.of(circulo), null));

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
