package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Distancia entre cada elemento de una capa base y cada uno de una capa de comparación: vista
 * {@code V_MEASURE_DISTANCE} de SIANTE (modelo {@code VMeasureDistance}), solo lectura. */
@Entity
@Immutable
@Table(name = "V_MEASURE_DISTANCE")
@IdClass(VistaDistanciaElementosEntity.Clave.class)
public class VistaDistanciaElementosEntity {

    @Id
    private Integer idFeatureBase;

    private Integer idLayerBase;

    private String jsonBase;

    @Id
    private Integer idFeatureComp;

    private Integer idLayerComp;

    private String jsonComp;

    private Integer distance;

    protected VistaDistanciaElementosEntity() {
    }

    public Integer getIdFeatureBase() {
        return idFeatureBase;
    }

    public Integer getIdLayerBase() {
        return idLayerBase;
    }

    public String getJsonBase() {
        return jsonBase;
    }

    public Integer getIdFeatureComp() {
        return idFeatureComp;
    }

    public Integer getIdLayerComp() {
        return idLayerComp;
    }

    public String getJsonComp() {
        return jsonComp;
    }

    public Integer getDistance() {
        return distance;
    }

    /** Clave inferida del modelo de SIANTE (la vista no tiene clave): leer solo con proyecciones. */
    public static class Clave implements Serializable {

        private Integer idFeatureBase;

        private Integer idFeatureComp;

        protected Clave() {
        }

        public Clave(Integer idFeatureBase, Integer idFeatureComp) {
            this.idFeatureBase = idFeatureBase;
            this.idFeatureComp = idFeatureComp;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c
                    && Objects.equals(idFeatureBase, c.idFeatureBase)
                    && Objects.equals(idFeatureComp, c.idFeatureComp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idFeatureBase, idFeatureComp);
        }
    }
}
