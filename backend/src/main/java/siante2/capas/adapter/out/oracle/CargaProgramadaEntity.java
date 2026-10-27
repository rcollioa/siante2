package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Carga de elementos pendiente de una capa: tabla {@code SCHEDULER_DATA} de SIATE (modelo {@code SchedulerSpatial}),
 * solo lectura. SIATE la llena con la metadata que supera 1.800.000 caracteres. */
@Entity
@Immutable
@Table(name = "SCHEDULER_DATA")
public class CargaProgramadaEntity {

    @Id
    private Integer id;

    private String data;

    private String status;

    private Integer layerId;

    private Integer lGroupId;

    protected CargaProgramadaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getData() {
        return data;
    }

    public String getStatus() {
        return status;
    }

    public Integer getLayerId() {
        return layerId;
    }

    public Integer getLGroupId() {
        return lGroupId;
    }
}
