package siante2.grupos.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Capa que pertenece a un grupo: tabla {@code LAYERS_GROUPS_LAYERS} de SIANTE (modelo {@code LayersGroupsLayers}), solo lectura. */
@Entity
@Immutable
@Table(name = "LAYERS_GROUPS_LAYERS")
@IdClass(GrupoCapasCapaEntity.Clave.class)
public class GrupoCapasCapaEntity {

    @Id
    @Column(name = "LAYER_ID")
    private Integer capaId;

    @Id
    @Column(name = "L_GROUP_ID")
    private Integer grupoId;

    @Column(name = "CREATED_DATE")
    private LocalDateTime fechaCreacion;

    protected GrupoCapasCapaEntity() {
    }

    public Integer getCapaId() {
        return capaId;
    }

    public Integer getGrupoId() {
        return grupoId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /** Clave compuesta: la tabla de SIANTE no tiene ID propio. */
    public static class Clave implements Serializable {

        private Integer capaId;

        private Integer grupoId;

        protected Clave() {
        }

        public Clave(Integer capaId, Integer grupoId) {
            this.capaId = capaId;
            this.grupoId = grupoId;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c
                    && Objects.equals(capaId, c.capaId)
                    && Objects.equals(grupoId, c.grupoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(capaId, grupoId);
        }
    }
}
