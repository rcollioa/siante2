package siante2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Perfil prod sobre H2: logs en JSON, sin contrato OpenAPI y con X-Request-Id en cada respuesta. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "prod"})
@ExtendWith(OutputCaptureExtension.class)
class PerfilProdTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void noPublicaElContratoYResponde404ConIdDeSolicitud() throws Exception {
        mockMvc.perform(get("/siante2/api/openapi"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void escribeLosLogsEnJsonEcs(CapturedOutput salida) {
        org.slf4j.LoggerFactory.getLogger(PerfilProdTest.class).info("prueba de formato");

        assertThat(salida.getOut()).contains("\"service\":{\"name\":\"siante2\",\"environment\":\"prod\"")
                .contains("\"message\":\"prueba de formato\"");
    }
}
