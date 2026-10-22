package siante2.usuarios.adapter.out.oracle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import siante2.usuarios.application.port.out.UsuariosPort;
import siante2.usuarios.domain.Usuario;

@Repository
class OracleUsuariosRepository implements UsuariosPort {

    private final UsuarioJpaRepository jpa;

    OracleUsuariosRepository(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Si hay duplicados por IV_USER, toma el de menor ID. */
    @Override
    public Optional<Usuario> buscarPorIvUser(String ivUser, List<String> grupos) {
        return jpa.buscarPorIvUser(ivUser).stream().findFirst().map(u -> new Usuario(
                u.getId(), u.getIvUser(), texto(u.getNombre()), texto(u.getApellido()), texto(u.getEmail()), grupos));
    }

    @Override
    public void registrar(String ivUser, List<String> grupos) {
        jpa.registrar(ivUser, String.join(",", grupos));
    }

    private static String texto(String valor) {
        return Objects.requireNonNullElse(valor, "");
    }
}
