package siante2.direcciones.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.compartido.adapter.in.web.UsuarioDePrueba;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.direcciones.application.port.in.BuscarDireccionesUseCase;
import siante2.direcciones.domain.Direccion;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DireccionesController.class)
@Import(UsuarioDePrueba.class)
class DireccionesControllerTest {

    private static final String RUTA = "/siante2/api/direcciones";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuscarDireccionesUseCase buscar;

    @Test
    void entregaLasDireccionesEncontradas() throws Exception {
        when(buscar.buscar("teatinos 56")).thenReturn(List.of(new Direccion("Teatinos 56, Santiago", -33.44, -70.65)));

        mockMvc.perform(get(RUTA).param("texto", "teatinos 56"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nombre").value("Teatinos 56, Santiago"))
                .andExpect(jsonPath("$.data[0].latitud").value(-33.44))
                .andExpect(jsonPath("$.data[0].longitud").value(-70.65));
    }

    @Test
    void sinTextoResponde400() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro 'texto'."));
    }

    @Test
    void siElServicioExternoFallaResponde503() throws Exception {
        when(buscar.buscar("santiago"))
                .thenThrow(new ServicioExternoNoDisponibleException("El servicio de búsqueda de direcciones no respondió.", null));

        mockMvc.perform(get(RUTA).param("texto", "santiago"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("El servicio de búsqueda de direcciones no respondió."));
    }
}
