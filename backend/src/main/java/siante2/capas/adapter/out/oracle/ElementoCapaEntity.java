package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Elemento (feature) de una capa: tabla {@code LAYERS_FEATURES} de SIANTE (modelo {@code LayerFeature}), solo lectura.
 * Sin {@code SPATIAL_OBJECT}: la geometría se lee como GML con consulta nativa. */
@Entity
@Immutable
@Table(name = "LAYERS_FEATURES")
public class ElementoCapaEntity {

    @Id
    private Integer idFeature;

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
}
