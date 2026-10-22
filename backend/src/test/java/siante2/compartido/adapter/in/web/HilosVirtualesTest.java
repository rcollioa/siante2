package siante2.compartido.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/** Tomcat real (no MockMvc): cada solicitud corre en un hilo virtual de Java 25. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HilosVirtualesTest {

    @LocalServerPort
    private int puerto;

    @Test
    void lasSolicitudesSeAtiendenEnHilosVirtuales() {
        String respuesta = RestClient.create("http://localhost:" + puerto).get()
                .uri("/siante2/api/prueba/hilo")
                .header(IdentidadWebSealFilter.CABECERA_USUARIO, "prueba")
                .header(IdentidadWebSealFilter.CABECERA_GRUPOS, "PRUEBA")
                .retrieve().body(String.class);

        assertThat(respuesta).isEqualTo("{\"valor\":\"true\"}");
    }
}
