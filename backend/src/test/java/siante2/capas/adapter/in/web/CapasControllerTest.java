package siante2.capas.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.domain.Capa;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.RecursoNoEncontradoException;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
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
                new Capa(5, "Bodegas", "", Visibilidad.PUBLICA, true, TipoCapa.VECTORIAL, List.of(circulo)));

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
