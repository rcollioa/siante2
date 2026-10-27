package siante2.grupos.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Grupo de capas: tabla {@code LAYERS_GROUPS} de SIATE (modelo {@code LayersGroups}), solo lectura. */
@Entity
@Immutable
@Table(name = "LAYERS_GROUPS")
public class GrupoCapasEntity {

    @Id
    private Integer id;

    private String name;

    private String description;

    private Integer owner;

    private Integer status;

    private LocalDateTime createdDate;

    private Integer isScheduler;

    private String createType;

    protected GrupoCapasEntity() {
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

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Integer getIsScheduler() {
        return isScheduler;
    }

    public String getCreateType() {
        return createType;
    }
}
