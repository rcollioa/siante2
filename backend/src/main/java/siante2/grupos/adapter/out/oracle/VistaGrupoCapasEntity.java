package siante2.grupos.adapter.out.oracle;

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
    private Integer id;

    private String name;

    private String description;

    private Integer owner;

    private Integer layersCount;

    private Integer status;

    private Integer isScheduler;

    private String createType;

    protected VistaGrupoCapasEntity() {
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

    public Integer getOwner() {
        return owner;
    }

    public Integer getLayersCount() {
        return layersCount;
    }

    public Integer getStatus() {
        return status;
    }

    public Integer getIsScheduler() {
        return isScheduler;
    }

    public String getCreateType() {
        return createType;
    }
}
