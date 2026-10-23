package siante2.direcciones.adapter.out.nominatim;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP de Nominatim con tiempo límite; respeta el proxy de la JVM ({@code -Dhttps.proxyHost}). */
@Configuration(proxyBeanMethods = false)
class NominatimConfig {

    @Bean
    RestClient nominatimRestClient(
            @Value("${app.geocodificacion.url}") String url,
            @Value("${app.geocodificacion.tiempo-limite}") Duration tiempoLimite,
            @Value("${app.geocodificacion.agente}") String agente) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(tiempoLimite)
                .proxy(ProxySelector.getDefault())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(tiempoLimite);
        // La política de uso de Nominatim exige identificar la aplicación.
        return RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(url)
                .defaultHeader(HttpHeaders.USER_AGENT, agente)
                .build();
    }
}
