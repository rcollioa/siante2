package siante2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Salud e info abiertas para k8s y para saber qué versión corre; métricas solo con usuario. */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void saludEInfoNoPidenUsuarioEInfoTraeLaVersion() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.nombre").value("SIATE"))
                .andExpect(jsonPath("$.build.version").exists());
    }

    @Test
    void metricasExigenUsuario() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").header("iv-user", "prueba").header("iv-groups", "PRUEBA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }
}
