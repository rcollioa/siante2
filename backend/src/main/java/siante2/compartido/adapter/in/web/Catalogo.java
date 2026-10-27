package siante2.compartido.adapter.in.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Controlador de datos que casi no cambian y son iguales para todos (categorías, multimedia): el navegador
 * los guarda {@code app.cache.max-age}. El resto de la API se revalida en cada consulta (ver CacheControlAdvice). */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Catalogo {
}
