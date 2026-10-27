package siante2.categorias.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.categorias.application.port.in.ListarCategoriasUseCase;
import siante2.categorias.domain.Categoria;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoriasController.class)
@Import(UsuarioDePrueba.class)
class CategoriasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarCategoriasUseCase listarCategorias;

    @Test
    void listaLasCategoriasActivasEnvueltasEnData() throws Exception {
        when(listarCategorias.listarActivas())
                .thenReturn(List.of(new Categoria(3, "Educación", "Establecimientos educacionales")));

        mockMvc.perform(get("/siante2/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].nombre").value("Educación"))
                .andExpect(jsonPath("$.data[0].descripcion").value("Establecimientos educacionales"));
    }

    @Test
    void sinCategoriasRespondeUnaListaVacia() throws Exception {
        when(listarCategorias.listarActivas()).thenReturn(List.of());

        mockMvc.perform(get("/siante2/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void esUnCatalogoQueGuardaSoloElNavegador() throws Exception {
        when(listarCategorias.listarActivas()).thenReturn(List.of());

        mockMvc.perform(get("/siante2/api/categorias"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, private"));
    }
}
