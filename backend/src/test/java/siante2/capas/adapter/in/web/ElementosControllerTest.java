package siante2.capas.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.capas.application.port.in.GuardarElementosUseCase;
import siante2.capas.application.port.in.ImportarCapaUseCase;
import siante2.capas.domain.CambiosElementos;
import siante2.capas.domain.CapaImportada;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.OrigenArchivo;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ElementosController.class)
@Import(UsuarioDePrueba.class)
class ElementosControllerTest {

    private static final String RUTA = "/siante2/api/capas/5/elementos";
    private static final String CAMBIOS = """
            {"nuevos":[{"id":-1,"geometria":{"type":"Point","coordinates":[-70.65,-33.44]},"radio":0,"nombre":"Bodega",
              "descripcion":"","estilo":{"color":"","grosor":0,"relleno":"","icono":""},"metadatos":[{"nombre":"Fuente","valor":"Terreno"}]}],
             "modificados":[],"eliminados":[3]}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuardarElementosUseCase guardar;

    @MockitoBean
    private ImportarCapaUseCase importar;

    @Test
    void importarResponde201ConLaCapaYElDuenoSaleDeWebSeal() throws Exception {
        ElementoCapa punto = new ElementoCapa(-1, Geometria.simple("Point", List.of(-70.65, -33.44)), 0, "Bodega", "",
                new EstiloElemento("", 0, "", ""), List.of());
        when(importar.importar("prueba", new CapaImportada("Bodegas", "Región", OrigenArchivo.KML, List.of(punto))))
                .thenReturn(new CapaPropia(50, "Bodegas", "Región", Visibilidad.PRIVADA, 0));

        mockMvc.perform(post("/siante2/api/capas/importar").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Bodegas","descripcion":"Región","origen":"KML","dueno":"otro","owner":9,
                         "elementos":[{"id":-1,"geometria":{"type":"Point","coordinates":[-70.65,-33.44]},"radio":0,"nombre":"Bodega",
                          "descripcion":"","estilo":{"color":"","grosor":0,"relleno":"","icono":""},"metadatos":[]}]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.visibilidad").value("PRIVADA"));
    }

    @Test
    void importarConDatosInvalidosResponde400ConElMensaje() throws Exception {
        when(importar.importar(eq("prueba"), any())).thenThrow(new SolicitudInvalidaException("El archivo no tiene geometrías."));

        mockMvc.perform(post("/siante2/api/capas/importar").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Bodegas","descripcion":"","origen":"GEOJSON","elementos":[]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El archivo no tiene geometrías."));
    }

    @Test
    void unTipoDeArchivoDesconocidoResponde400() throws Exception {
        mockMvc.perform(post("/siante2/api/capas/importar").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Bodegas","descripcion":"","origen":"SHP","elementos":[]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void guardaLosCambiosConElUsuarioDeWebSealYEntregaLosIdDeLosNuevos() throws Exception {
        ElementoCapa nuevo = new ElementoCapa(-1, Geometria.simple("Point", List.of(-70.65, -33.44)), 0, "Bodega", "",
                new EstiloElemento("", 0, "", ""), List.of(new Metadato("Fuente", "Terreno")));
        when(guardar.guardar(5, "prueba", new CambiosElementos(List.of(nuevo), List.of(), List.of(3)))).thenReturn(List.of(12));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CAMBIOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nuevos[0]").value(12));
    }

    @Test
    void unaCapaQueNoSePuedeEditarResponde404() throws Exception {
        when(guardar.guardar(eq(5), eq("prueba"), any())).thenThrow(new RecursoNoEncontradoException("La capa no existe."));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CAMBIOS))
                .andExpect(status().isNotFound());
    }

    @Test
    void unDatoInvalidoResponde400ConElMensaje() throws Exception {
        when(guardar.guardar(eq(5), eq("prueba"), any())).thenThrow(new SolicitudInvalidaException("El color de un elemento no es válido."));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CAMBIOS))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El color de un elemento no es válido."));
    }
}
