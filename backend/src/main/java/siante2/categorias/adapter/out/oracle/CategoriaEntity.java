package siante2.categorias.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Tabla {@code CATEGORIES} de SIATE (modelo {@code Category}). Solo lectura mientras
 * SIATE siga escribiendo en la misma base. */
@Entity
@Immutable
@Table(name = "CATEGORIES")
public class CategoriaEntity {

    /** Valor de {@code STATUS} para registros activos ({@code REGISTER_STATUS_ACTIVE} en SIATE). */
    public static final String ACTIVA = "1";

    @Id
    private Integer id;

    private String name;

    private String description;

    private String status;

    protected CategoriaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}
