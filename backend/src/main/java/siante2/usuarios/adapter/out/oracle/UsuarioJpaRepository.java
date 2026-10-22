package siante2.usuarios.adapter.out.oracle;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Integer> {

    @Query("select u from UsuarioEntity u where u.ivUser = :ivUser order by u.id")
    List<UsuarioEntity> buscarPorIvUser(String ivUser);

    /** Mismas columnas que el alta de SIANTE; el ID lo asigna la base. */
    @Modifying
    @Transactional
    @Query(value = "insert into USERS (NAME, IV_USER, IV_GROUP) values (:ivUser, :ivUser, :grupos)", nativeQuery = true)
    void registrar(String ivUser, String grupos);
}
