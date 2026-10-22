package siante2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Levanta el contexto completo contra H2 para detectar en el build errores de cableado
 * (beans faltantes, repositorios mal declarados, propiedades inválidas). */
@SpringBootTest
class ContextoAplicacionTest {

    @Test
    void elContextoArranca() {
        // Basta con que el contexto se construya sin errores.
    }
}
