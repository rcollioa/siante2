package siante2.usuarios.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import siante2.usuarios.application.port.out.UsuariosPort;
import siante2.usuarios.domain.Usuario;

import static org.assertj.core.api.Assertions.assertThat;

class UsuariosServiceTest {

    /** Doble en memoria del puerto: el ID lo asigna "la base" al registrar. */
    private static class UsuariosEnMemoria implements UsuariosPort {
        final List<String> registrados = new ArrayList<>();

        @Override
        public Optional<Usuario> buscarPorIvUser(String ivUser, List<String> grupos) {
            int indice = registrados.indexOf(ivUser);
            return indice < 0 ? Optional.empty()
                    : Optional.of(new Usuario(indice + 1, ivUser, ivUser, "", "", grupos));
        }

        @Override
        public void registrar(String ivUser, List<String> grupos) {
            registrados.add(ivUser);
        }
    }

    @Test
    void registraAlUsuarioLaPrimeraVezYLuegoLoReutiliza() {
        UsuariosEnMemoria puerto = new UsuariosEnMemoria();
        UsuariosService servicio = new UsuariosService(puerto);

        Usuario primero = servicio.obtener("jperez", List.of("SIATE"));
        Usuario segundo = servicio.obtener("jperez", List.of("SIATE"));

        assertThat(puerto.registrados).containsExactly("jperez");
        assertThat(primero).isEqualTo(segundo);
        assertThat(primero.nombre()).isEqualTo("jperez");
    }
}
