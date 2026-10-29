package siante2.capas.adapter.out.wms;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP de los servidores de mapas de las capas por URL: tiempo límite, proxy de la JVM y sin seguir
 * redirecciones, para que un servidor no lleve al backend a otra dirección. */
@Configuration(proxyBeanMethods = false)
class WmsConfig {

    @Bean
    RestClient serviciosMapaRestClient(
            @Value("${app.servicios-mapa.tiempo-limite}") Duration tiempoLimite,
            @Value("${app.geocodificacion.agente}") String agente) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(tiempoLimite)
                .followRedirects(HttpClient.Redirect.NEVER)
                .proxy(ProxySelector.getDefault())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(tiempoLimite);
        return RestClient.builder()
                .requestFactory(fabrica)
                .defaultHeader(HttpHeaders.USER_AGENT, agente)
                .build();
    }
}
