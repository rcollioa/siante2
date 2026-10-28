package siante2.usuarios.adapter.out.oracle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.usuarios.application.port.out.UsuariosPort;
import siante2.usuarios.domain.Usuario;

@Repository
@Transactional(readOnly = true)
class OracleUsuariosRepository implements UsuariosPort {

    /** {@code USERS.IV_GROUP} es {@code VARCHAR2(1000)}. */
    static final int LARGO_IV_GROUP = 1000;
    private static final Logger log = LoggerFactory.getLogger(OracleUsuariosRepository.class);

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
        jpa.save(new UsuarioEntity(ivUser, gruposQueCaben(grupos)));
    }

    /** Los grupos que caben completos en IV_GROUP, en su orden. Basta como registro, igual que en SIATE: los
     * permisos salen de iv-groups en cada solicitud, no de esta columna. */
    static String gruposQueCaben(List<String> grupos) {
        StringBuilder texto = new StringBuilder();
        int guardados = 0;
        for (String grupo : grupos) {
            int largo = texto.isEmpty() ? grupo.length() : texto.length() + 1 + grupo.length();
            if (largo > LARGO_IV_GROUP) {
                break;
            }
            if (!texto.isEmpty()) {
                texto.append(',');
            }
            texto.append(grupo);
            guardados++;
        }
        if (guardados < grupos.size()) {
            log.warn("IV_GROUP guarda {} de {} grupos: el resto no cabe en {} caracteres",
                    guardados, grupos.size(), LARGO_IV_GROUP);
        }
        return texto.toString();
    }

    private static String texto(String valor) {
        return Objects.requireNonNullElse(valor, "");
    }
}
