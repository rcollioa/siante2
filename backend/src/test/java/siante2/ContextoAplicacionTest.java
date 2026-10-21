package siante2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Levanta el contexto completo (web, JPA, Hikari, actuator) contra H2 en memoria (ver
 * {@code src/test/resources/application.properties}). Atrapa en el build los errores de cableado que si
 * no recién aparecen al desplegar: un bean sin implementación, un repositorio JPA mal declarado, una
 * propiedad con formato inválido.
 */
@SpringBootTest
class ContextoAplicacionTest {

    @Test
    void elContextoArranca() {
        // Basta con que el contexto se construya sin errores.
    }
}
