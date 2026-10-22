package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Column;
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
    @Column(name = "LAYER_ID")
    private Integer capaId;

    @Column(name = "CATEGORY_ID")
    private Integer categoriaId;

    protected CapaCategoriaEntity() {
    }

    public Integer getCapaId() {
        return capaId;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }
}
