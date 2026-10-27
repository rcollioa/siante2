package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Elementos de capa filtrados por tipo para la intersección: vista {@code V_LF_TYPE_FILTER} de SIATE (modelo
 * {@code VLfTypeFilter}), solo lectura. Sin {@code SPATIAL_OBJECT}: la geometría se lee como GML con consulta nativa. */
@Entity
@Immutable
@Table(name = "V_LF_TYPE_FILTER")
@IdClass(VistaElementoFiltradoEntity.Clave.class)
public class VistaElementoFiltradoEntity {

    @Id
    private Integer idFeature;

    @Id
    private Integer idLayer;

    private String jsonMetadata;

    protected VistaElementoFiltradoEntity() {
    }

    public Integer getIdFeature() {
        return idFeature;
    }

    public Integer getIdLayer() {
        return idLayer;
    }

    public String getJsonMetadata() {
        return jsonMetadata;
    }

    /** Clave inferida del modelo de SIATE (la vista no tiene clave): leer solo con proyecciones. */
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
