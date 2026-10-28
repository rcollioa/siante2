package siante2;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Actuator en su propio puerto, como en k8s: salud e info abiertas para las sondas y para saber qué versión
 * corre; métricas solo con usuario; nada de Actuator en el puerto de la API. Servidores reales en puertos al
 * azar, porque con MockMvc no hay segundo puerto que probar. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
class ActuatorTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.management.port}")
    private int puertoAdministracion;

    @Value("${local.server.port}")
    private int puertoApi;

    @Test
    void saludEInfoNoPidenUsuarioEInfoTraeLaVersion() throws Exception {
        assertThat(get(puertoAdministracion, "/actuator/health/liveness").statusCode()).isEqualTo(200);
        assertThat(get(puertoAdministracion, "/actuator/health/readiness").statusCode()).isEqualTo(200);

        HttpResponse<String> info = get(puertoAdministracion, "/actuator/info");
        assertThat(info.statusCode()).isEqualTo(200);
        assertThat(info.body()).contains("\"nombre\":\"SIATE\"").contains("\"build\"");
    }

    @Test
    void metricasExigenUsuario() throws Exception {
        assertThat(get(puertoAdministracion, "/actuator/metrics").statusCode()).isEqualTo(401);

        HttpResponse<String> metricas = get(puertoAdministracion, "/actuator/metrics", "iv-user", "prueba", "iv-groups", "PRUEBA");
        assertThat(metricas.statusCode()).isEqualTo(200);
        assertThat(metricas.body()).contains("\"names\"");
    }

    @Test
    void elPuertoDeLaApiNoTieneActuator() throws Exception {
        assertThat(get(puertoApi, "/actuator/health", "iv-user", "prueba", "iv-groups", "PRUEBA").statusCode()).isEqualTo(404);
        assertThat(get(puertoApi, "/siante2/api/actuator/health", "iv-user", "prueba", "iv-groups", "PRUEBA").statusCode())
                .isEqualTo(404);
    }

    private HttpResponse<String> get(int puerto, String ruta, String... cabeceras) throws IOException, InterruptedException {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
        if (cabeceras.length > 0) {
            pedido.headers(cabeceras);
        }
        return http.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }
}
