package siante2.compartido.adapter.in.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El intercambio real de Angular, sin el token simulado de {@link UsuarioDePrueba}: el GET deja la cookie
 * {@code XSRF-TOKEN} y la escritura la devuelve en la cabecera. */
@WebMvcTest(ControladorPrueba.class)
@Import(SeguridadConfig.class)
class CsrfTest {

    private static final String RUTA = "/siante2/api/prueba";

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder conUsuario(MockHttpServletRequestBuilder solicitud) {
        return solicitud.header(IdentidadWebSealFilter.CABECERA_USUARIO, "prueba")
                .header(IdentidadWebSealFilter.CABECERA_GRUPOS, "PRUEBA");
    }

    private static MockHttpServletRequestBuilder escritura() {
        return conUsuario(post(RUTA + "/cuerpo")).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"x\"}");
    }

    @Test
    void sinElTokenUnaEscrituraResponde403() throws Exception {
        mockMvc.perform(escritura())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void elGetEntregaUnaCookieQueElNavegadorPuedeLeerYLaEscrituraLaDevuelve() throws Exception {
        Cookie cookie = mockMvc.perform(conUsuario(get(RUTA))).andReturn().getResponse().getCookie("XSRF-TOKEN");

        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isFalse();
        mockMvc.perform(escritura().cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue()))
                .andExpect(status().isOk());
    }

    @Test
    void unTokenDistintoAlDeLaCookieResponde403() throws Exception {
        Cookie cookie = mockMvc.perform(conUsuario(get(RUTA))).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(escritura().cookie(cookie).header("X-XSRF-TOKEN", "otro"))
                .andExpect(status().isForbidden());
    }
}
