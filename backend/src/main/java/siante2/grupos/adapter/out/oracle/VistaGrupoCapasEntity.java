package siante2.grupos.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Vista de grupos con su cantidad de capas: tabla {@code V_LAYERS_GROUPS} de SIANTE (modelo {@code VLayersGroups}), solo lectura. */
@Entity
@Immutable
@Table(name = "V_LAYERS_GROUPS")
public class VistaGrupoCapasEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "DESCRIPTION")
    private String descripcion;

    @Column(name = "OWNER")
    private Integer dueno;

    @Column(name = "LAYERS_COUNT")
    private Integer cantidadCapas;

    @Column(name = "STATUS")
    private Integer estado;

    @Column(name = "IS_SCHEDULER")
    private Integer programado;

    @Column(name = "CREATE_TYPE")
    private String tipoCreacion;

    protected VistaGrupoCapasEntity() {
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

    public Integer getCantidadCapas() {
        return cantidadCapas;
    }

    public Integer getEstado() {
        return estado;
    }

    public Integer getProgramado() {
        return programado;
    }

    public String getTipoCreacion() {
        return tipoCreacion;
    }
}
