package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Capa: tabla {@code LAYERS} de SIANTE (modelo {@code Layer}), solo lectura.
 * Sin {@code FILE_BASE64}: el archivo original se lee aparte, solo cuando se necesita. */
@Entity
@Immutable
@Table(name = "LAYERS")
public class CapaEntity {

    @Id
    private Integer id;

    private Integer owner;

    private String name;

    private String origin;

    private String status;

    private String jsonMetadata;

    private String description;

    private String visibility;

    private Integer lGroupId;

    private Integer categoryId;

    protected CapaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getOrigin() {
        return origin;
    }

    public String getStatus() {
        return status;
    }

    public String getJsonMetadata() {
        return jsonMetadata;
    }

    public String getDescription() {
        return description;
    }

    public String getVisibility() {
        return visibility;
    }

    public Integer getLGroupId() {
        return lGroupId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }
}
