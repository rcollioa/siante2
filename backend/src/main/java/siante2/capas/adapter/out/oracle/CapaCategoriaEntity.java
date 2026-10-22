package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Categoría asignada a una capa: tabla {@code LAYERS_CATEGORIES} de SIANTE (modelo {@code LayersCategories}), solo lectura. */
@Entity
@Immutable
@Table(name = "LAYERS_CATEGORIES")
public class CapaCategoriaEntity {

    @Id
    private Integer layerId;

    private Integer categoryId;

    protected CapaCategoriaEntity() {
    }

    public Integer getLayerId() {
        return layerId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }
}
