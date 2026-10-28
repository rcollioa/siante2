package siante2.usuarios.adapter.in.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import siante2.compartido.adapter.in.web.SeguridadConfig;
import siante2.usuarios.application.port.in.ObtenerUsuarioActualUseCase;
import siante2.usuarios.domain.Usuario;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        when(usuarioActual.obtener("jperez", List.of("SIATE", "CGR")))
                .thenReturn(new Usuario(5, "jperez", "Juan", "Pérez", "jperez@contraloria.cl", List.of("SIATE", "CGR")));

        mockMvc.perform(get("/siante2/api/usuarios/actual")
                        .header("iv-user", "jperez").header("iv-groups", "SIATE, CGR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.grupos[1]").value("CGR"));
    }

    @Test
    void losDatosDelUsuarioNoQuedanEnCachesCompartidasYSeRevalidan() throws Exception {
        when(usuarioActual.obtener("jperez", List.of()))
                .thenReturn(new Usuario(5, "jperez", "Juan", "Pérez", "jperez@contraloria.cl", List.of()));

        mockMvc.perform(get("/siante2/api/usuarios/actual").header("iv-user", "jperez").header("iv-groups", ""))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, private"));
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

    @Test
    void conGruposVaciosComoEnSianteAutenticaAlUsuarioSinGrupos() throws Exception {
        when(usuarioActual.obtener("jperez", List.of()))
                .thenReturn(new Usuario(5, "jperez", "Juan", "Pérez", "jperez@contraloria.cl", List.of()));

        mockMvc.perform(get("/siante2/api/usuarios/actual").header("iv-user", "jperez").header("iv-groups", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos").isEmpty());
    }

    @Test
    void sinCabeceraDeGruposResponde401() throws Exception {
        mockMvc.perform(get("/siante2/api/usuarios/actual").header("iv-user", "jperez"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conSaltoDeLineaEnElUsuarioElFirewallDeSpringSecurityResponde400() throws Exception {
        mockMvc.perform(get("/siante2/api/usuarios/actual")
                        .header("iv-user", "jperez\n[] [admin] acceso concedido").header("iv-groups", "SIATE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conUsuarioMasLargoQueUsersIvUserResponde401() throws Exception {
        mockMvc.perform(get("/siante2/api/usuarios/actual")
                        .header("iv-user", "a".repeat(21)).header("iv-groups", "SIATE"))
                .andExpect(status().isUnauthorized());
    }
}
