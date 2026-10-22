package siante2.usuarios.adapter.out.oracle;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Integer> {

    /** Si hay duplicados por IV_USER, el de menor ID; la base devuelve una sola fila. */
    Optional<UsuarioEntity> findFirstByIvUserOrderByIdAsc(String ivUser);
}
