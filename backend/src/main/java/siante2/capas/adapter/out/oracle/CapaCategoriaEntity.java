package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Categoría asignada a una capa: tabla {@code LAYERS_CATEGORIES} de SIATE (modelo {@code LayersCategories}), solo
 * lectura. Clave {@code (LAYER_ID, CATEGORY_ID)}: una capa puede tener varias categorías. */
@Entity
@Immutable
@Table(name = "LAYERS_CATEGORIES")
@IdClass(CapaCategoriaEntity.Clave.class)
public class CapaCategoriaEntity {

    @Id
    private Integer layerId;

    @Id
    private Integer categoryId;

    protected CapaCategoriaEntity() {
    }

    public Integer getLayerId() {
        return layerId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    /** Clave compuesta, la misma de la clave primaria de Oracle. */
    public static class Clave implements Serializable {

        private Integer layerId;

        private Integer categoryId;

        protected Clave() {
        }

        public Clave(Integer layerId, Integer categoryId) {
            this.layerId = layerId;
            this.categoryId = categoryId;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c
                    && Objects.equals(layerId, c.layerId)
                    && Objects.equals(categoryId, c.categoryId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(layerId, categoryId);
        }
    }
}
