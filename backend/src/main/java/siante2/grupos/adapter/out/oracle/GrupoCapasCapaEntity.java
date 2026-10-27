package siante2.grupos.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Capa que pertenece a un grupo: tabla {@code LAYERS_GROUPS_LAYERS} de SIATE (modelo {@code LayersGroupsLayers}), solo lectura. */
@Entity
@Immutable
@Table(name = "LAYERS_GROUPS_LAYERS")
@IdClass(GrupoCapasCapaEntity.Clave.class)
public class GrupoCapasCapaEntity {

    @Id
    private Integer layerId;

    @Id
    private Integer lGroupId;

    private LocalDateTime createdDate;

    protected GrupoCapasCapaEntity() {
    }

    public Integer getLayerId() {
        return layerId;
    }

    public Integer getLGroupId() {
        return lGroupId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /** Clave compuesta: la tabla de SIATE no tiene ID propio. */
    public static class Clave implements Serializable {

        private Integer layerId;

        private Integer lGroupId;

        protected Clave() {
        }

        public Clave(Integer layerId, Integer lGroupId) {
            this.layerId = layerId;
            this.lGroupId = lGroupId;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c
                    && Objects.equals(layerId, c.layerId)
                    && Objects.equals(lGroupId, c.lGroupId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(layerId, lGroupId);
        }
    }
}
