package siante2.grupos.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Grupo de capas: tabla {@code LAYERS_GROUPS} de SIANTE (modelo {@code LayersGroups}), solo lectura. */
@Entity
@Immutable
@Table(name = "LAYERS_GROUPS")
public class GrupoCapasEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "DESCRIPTION")
    private String descripcion;

    @Column(name = "OWNER")
    private Integer dueno;

    @Column(name = "STATUS")
    private Integer estado;

    @Column(name = "CREATED_DATE")
    private LocalDateTime fechaCreacion;

    @Column(name = "IS_SCHEDULER")
    private Integer programado;

    @Column(name = "CREATE_TYPE")
    private String tipoCreacion;

    protected GrupoCapasEntity() {
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

    public Integer getDueno() {
        return dueno;
    }

    public Integer getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Integer getProgramado() {
        return programado;
    }

    public String getTipoCreacion() {
        return tipoCreacion;
    }
}
