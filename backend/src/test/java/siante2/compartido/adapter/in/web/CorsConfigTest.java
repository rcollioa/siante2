package siante2.compartido.adapter.in.web;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CORS de la API ({@link CorsConfig}), sobre {@link ControladorPrueba}. */
class CorsConfigTest {

    private static final String RUTA = "/siante2/api/prueba";

    @Nested
    @WebMvcTest(value = ControladorPrueba.class, properties = "app.cors.allowed-origins=https://permitido.cl,https://otro.cl")
    class ConOrigenesConfigurados {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void unOrigenDeLaListaRecibeLaCabeceraSinCredenciales() throws Exception {
            mockMvc.perform(get(RUTA).header(HttpHeaders.ORIGIN, "https://otro.cl"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://otro.cl"))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        }

        @Test
        void unOrigenFueraDeLaListaSeRechaza() throws Exception {
            mockMvc.perform(get(RUTA).header(HttpHeaders.ORIGIN, "https://ajeno.cl"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void laConsultaPreviaSoloPermiteGetYPost() throws Exception {
            mockMvc.perform(options(RUTA)
                            .header(HttpHeaders.ORIGIN, "https://permitido.cl")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @WebMvcTest(ControladorPrueba.class)
    class SinOrigenesConfigurados {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void noSeAgregaNingunaCabeceraYElNavegadorBloqueaLaRespuesta() throws Exception {
            mockMvc.perform(get(RUTA).header(HttpHeaders.ORIGIN, "https://ajeno.cl"))
                    .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        }
    }
}
