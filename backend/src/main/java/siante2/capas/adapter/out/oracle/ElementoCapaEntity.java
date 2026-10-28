package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Elemento (feature) de una capa: tabla {@code LAYERS_FEATURES} de SIATE (modelo {@code LayerFeature}), solo lectura.
 * Sin {@code SPATIAL_OBJECT}: la geometría se lee como GML con consulta nativa. Clave {@code (ID_FEATURE, ID_LAYER)}:
 * SIATE numera los elementos dentro de cada capa. */
@Entity
@Immutable
@Table(name = "LAYERS_FEATURES")
@IdClass(ElementoCapaEntity.Clave.class)
public class ElementoCapaEntity {

    @Id
    private Integer idFeature;

    @Id
    private Integer idLayer;

    private String type;

    private LocalDateTime dateCreate;

    private LocalDateTime dateModify;

    private String jsonMetadata;

    protected ElementoCapaEntity() {
    }

    public Integer getIdFeature() {
        return idFeature;
    }

    public Integer getIdLayer() {
        return idLayer;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getDateCreate() {
        return dateCreate;
    }

    public LocalDateTime getDateModify() {
        return dateModify;
    }

    public String getJsonMetadata() {
        return jsonMetadata;
    }

    /** Clave compuesta, la misma de la clave primaria de Oracle. */
    public static class Clave implements Serializable {

        private Integer idFeature;

        private Integer idLayer;

        protected Clave() {
        }

        public Clave(Integer idFeature, Integer idLayer) {
            this.idFeature = idFeature;
            this.idLayer = idLayer;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c
                    && Objects.equals(idFeature, c.idFeature)
                    && Objects.equals(idLayer, c.idLayer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idFeature, idLayer);
        }
    }
}
