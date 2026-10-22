package siante2.usuarios.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.compartido.adapter.in.web.SeguridadConfig;
import siante2.usuarios.application.port.in.ObtenerUsuarioActualUseCase;
import siante2.usuarios.domain.Usuario;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuariosController.class)
@Import(SeguridadConfig.class)
class UsuariosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ObtenerUsuarioActualUseCase usuarioActual;

    @Test
    void devuelveElUsuarioDeLasCabecerasDeWebSeal() throws Exception {
        when(usuarioActual.obtener("jperez", List.of("SIANTE", "CGR")))
                .thenReturn(new Usuario(5, "jperez", "Juan", "Pérez", "jperez@contraloria.cl", List.of("SIANTE", "CGR")));

        mockMvc.perform(get("/siante2/api/usuarios/actual")
                        .header("iv-user", "jperez").header("iv-groups", "SIANTE, CGR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.grupos[1]").value("CGR"));
    }

    @Test
    void sinCabecerasResponde401ConElFormatoDeErrores() throws Exception {
        mockMvc.perform(get("/siante2/api/usuarios/actual"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void conUsuarioUnauthenticatedDeWebSealResponde401() throws Exception {
        mockMvc.perform(get("/siante2/api/usuarios/actual")
                        .header("iv-user", "Unauthenticated").header("iv-groups", "Unauthenticated"))
                .andExpect(status().isUnauthorized());
    }
}
