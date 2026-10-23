package siante2.compartido.adapter.out;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Habilita {@code @Retryable} y {@code @ConcurrencyLimit} (Spring Framework 7) para servicios externos. */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class ResilienciaConfig {
}
