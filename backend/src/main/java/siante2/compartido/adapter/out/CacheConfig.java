package siante2.compartido.adapter.out;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Habilita {@code @Cacheable} en los adaptadores de salida. Cachés y vigencia en application.properties
 * ({@code spring.cache.*}); cada réplica tiene la suya, así que solo para datos que casi no cambian. */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {
}
