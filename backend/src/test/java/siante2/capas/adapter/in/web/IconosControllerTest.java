package siante2.capas.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.capas.application.port.in.CrearIconoUseCase;
import siante2.capas.application.port.in.EditarIconoUseCase;
import siante2.capas.application.port.in.EliminarIconoUseCase;
import siante2.capas.application.port.in.ListarIconosUseCase;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IconosController.class)
@Import(UsuarioDePrueba.class)
class IconosControllerTest {

    private static final String RUTA = "/siante2/api/iconos";
    private static final String PNG = "data:image/png;base64,iVBORw0KGgo=";
    private static final String CUERPO = """
            {"nombre":"Bodega","archivo":"bodega.png","imagen":"%s","owner":9}""".formatted(PNG);
    private static final DatosIcono DATOS = new DatosIcono("Bodega", "bodega.png", PNG);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarIconosUseCase listar;

    @MockitoBean
    private CrearIconoUseCase crear;

    @MockitoBean
    private EditarIconoUseCase editar;

    @MockitoBean
    private EliminarIconoUseCase eliminar;

    @Test
    void listaDentroDeDataParaElUsuarioDeWebSeal() throws Exception {
        when(listar.listar("prueba")).thenReturn(List.of(new Icono(5, "Bodega", "bodega.png", PNG, true)));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[0].imagen").value(PNG))
                .andExpect(jsonPath("$.data[0].propio").value(true));
    }

    @Test
    void crearResponde201ConElIconoYElDuenoSaleDeWebSeal() throws Exception {
        when(crear.crear("prueba", DATOS)).thenReturn(new Icono(50, "Bodega", "bodega.png", PNG, true));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    void crearInvalidoResponde400ConElMensaje() throws Exception {
        when(crear.crear("prueba", DATOS)).thenThrow(new SolicitudInvalidaException("Nombre requerido."));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nombre requerido."));
    }

    @Test
    void editarResponde204() throws Exception {
        mockMvc.perform(patch(RUTA + "/5").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNoContent());

        verify(editar).editar(5, "prueba", DATOS);
    }

    @Test
    void eliminarResponde204() throws Exception {
        mockMvc.perform(post(RUTA + "/5/eliminar")).andExpect(status().isNoContent());

        verify(eliminar).eliminar(5, "prueba");
    }

    @Test
    void eliminarUnoAjenoResponde404() throws Exception {
        doThrow(new RecursoNoEncontradoException("El ícono no existe.")).when(eliminar).eliminar(6, "prueba");

        mockMvc.perform(post(RUTA + "/6/eliminar")).andExpect(status().isNotFound());
    }
}
