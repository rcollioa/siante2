package siante2.usuarios.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Tabla {@code USERS} de SIATE (modelo {@code User}). Se puede dar de alta, no modificar (@Immutable);
 * el ID lo pone el trigger de la base y Hibernate lo recupera con RETURNING (IDENTITY). */
@Entity
@Immutable
@Table(name = "USERS")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String lastName;

    private String email;

    private String area;

    private String ivUser;

    private String ivGroup;

    protected UsuarioEntity() {
    }

    /** Mismas columnas que el alta de {@code Auth} en SIATE: el nombre parte siendo el iv-user. */
    UsuarioEntity(String ivUser, String grupos) {
        this.name = ivUser;
        this.ivUser = ivUser;
        this.ivGroup = grupos;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getIvUser() {
        return ivUser;
    }
}
