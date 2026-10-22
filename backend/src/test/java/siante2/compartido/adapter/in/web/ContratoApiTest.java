package siante2.compartido.adapter.in.web;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Falla si la API cambió y el contrato versionado no. Para actualizarlo:
 * {@code mvnw test -Dtest=ContratoApiTest "-Dcontrato.actualizar=true"} */
@SpringBootTest(properties = "springdoc.paths-to-exclude=/siante2/api/prueba/**")
@AutoConfigureMockMvc
class ContratoApiTest {

    private static final Path CONTRATO = Path.of("src/test/resources/contrato/openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void elContratoVersionadoCoincideConLaApi() throws Exception {
        String actual = normalizar(mockMvc.perform(get("/siante2/api/openapi"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        if (Boolean.getBoolean("contrato.actualizar")) {
            Files.createDirectories(CONTRATO.getParent());
            Files.writeString(CONTRATO, actual);
            return;
        }
        assertThat(CONTRATO).as("Falta el contrato; generarlo con -Dcontrato.actualizar=true").exists();
        assertThat(actual)
                .as("La API cambió; revisar y regenerar el contrato con -Dcontrato.actualizar=true")
                .isEqualTo(normalizar(Files.readString(CONTRATO)));
    }

    private static String normalizar(String json) {
        return json.replace("\r\n", "\n").strip() + "\n";
    }
}
