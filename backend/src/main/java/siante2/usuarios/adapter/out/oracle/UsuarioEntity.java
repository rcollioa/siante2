package siante2.usuarios.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Tabla {@code USERS} de SIANTE (modelo {@code User}). El alta va por consulta nativa: el ID lo pone la base. */
@Entity
@Immutable
@Table(name = "USERS")
public class UsuarioEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "LAST_NAME")
    private String apellido;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "AREA")
    private String area;

    @Column(name = "IV_USER")
    private String ivUser;

    @Column(name = "IV_GROUP")
    private String ivGroup;

    protected UsuarioEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getIvUser() {
        return ivUser;
    }
}
