package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Column;
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
    @Column(name = "ID_FEATURE")
    private Integer id;

    @Column(name = "ID_LAYER")
    private Integer capaId;

    @Column(name = "TYPE")
    private String tipo;

    @Column(name = "DATE_CREATE")
    private LocalDateTime fechaCreacion;

    @Column(name = "DATE_MODIFY")
    private LocalDateTime fechaModificacion;

    @Column(name = "JSON_METADATA")
    private String metadatosJson;

    protected ElementoCapaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getCapaId() {
        return capaId;
    }

    public String getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public String getMetadatosJson() {
        return metadatosJson;
    }
}
