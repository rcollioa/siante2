package siante2.compartido.adapter.in.web;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VersionController.class)
@Import({UsuarioDePrueba.class, VersionControllerTest.Compilacion.class})
class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class Compilacion {

        @Bean
        BuildProperties buildProperties() {
            Properties propiedades = new Properties();
            propiedades.setProperty("version", "1.2.0");
            propiedades.setProperty("time", "2026-10-29T17:28:03Z");
            return new BuildProperties(propiedades);
        }
    }

    @Test
    void entregaLaVersionYLaFechaDeCompilacion() throws Exception {
        mockMvc.perform(get("/siante2/api/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.2.0"))
                .andExpect(jsonPath("$.compilada").value("2026-10-29T17:28:03Z"));
    }

    @Test
    void sinBuildInfoLoDiceEnVezDeFallar() {
        var sinCompilacion = new StaticListableBeanFactory().getBeanProvider(BuildProperties.class);

        assertThat(new VersionController(sinCompilacion).obtenerVersion())
                .isEqualTo(new VersionController.Version("desconocida", null));
    }
}
