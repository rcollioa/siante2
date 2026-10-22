package siante2.categorias.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Tabla {@code CATEGORIES} de SIANTE (modelo {@code Category}). Solo lectura mientras
 * SIANTE siga escribiendo en la misma base. */
@Entity
@Immutable
@Table(name = "CATEGORIES")
public class CategoriaEntity {

    /** Valor de {@code STATUS} para registros activos ({@code REGISTER_STATUS_ACTIVE} en SIANTE). */
    public static final String ACTIVA = "1";

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "DESCRIPTION")
    private String descripcion;

    @Column(name = "STATUS")
    private String estado;

    protected CategoriaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEstado() {
        return estado;
    }
}
