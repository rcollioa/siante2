package siante2.usuarios.adapter.out.oracle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.usuarios.application.port.out.UsuariosPort;
import siante2.usuarios.domain.Usuario;

@Repository
@Transactional(readOnly = true)
class OracleUsuariosRepository implements UsuariosPort {

    private final UsuarioJpaRepository jpa;

    OracleUsuariosRepository(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Usuario> buscarPorIvUser(String ivUser, List<String> grupos) {
        return jpa.findFirstByIvUserOrderByIdAsc(ivUser).map(u -> new Usuario(
                u.getId(), u.getIvUser(), texto(u.getName()), texto(u.getLastName()), texto(u.getEmail()), grupos));
    }

    @Override
    @Transactional
    public void registrar(String ivUser, List<String> grupos) {
        jpa.save(new UsuarioEntity(ivUser, String.join(",", grupos)));
    }

    private static String texto(String valor) {
        return Objects.requireNonNullElse(valor, "");
    }
}
