package siante2.direcciones.adapter.out.photon;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP de Photon con tiempo límite corto (sugerir no puede hacer esperar); respeta el proxy de la JVM. */
@Configuration(proxyBeanMethods = false)
class PhotonConfig {

    @Bean
    RestClient photonRestClient(
            @Value("${app.autocompletado.url}") String url,
            @Value("${app.autocompletado.tiempo-limite}") Duration tiempoLimite,
            @Value("${app.geocodificacion.agente}") String agente) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(tiempoLimite)
                .proxy(ProxySelector.getDefault())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(tiempoLimite);
        return RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(url)
                .defaultHeader(HttpHeaders.USER_AGENT, agente)
                .build();
    }
}
