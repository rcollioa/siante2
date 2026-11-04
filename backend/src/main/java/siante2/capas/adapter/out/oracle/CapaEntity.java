package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Capa: tabla {@code LAYERS} de SIATE (modelo {@code Layer}). Sin {@code @Immutable} porque se escribe: se da de alta y
 * se actualiza solo con los {@code update} de {@link CapaJpaRepository}; sin setters, para que nunca se reescriba la
 * fila completa, que SIATE también escribe. El ID lo pone el trigger de la base y Hibernate lo recupera (IDENTITY). Sin
 * {@code FILE_BASE64}: el archivo original se lee aparte, solo cuando se necesita. */
@Entity
@Table(name = "LAYERS")
public class CapaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    /** Lo que guarda SIATE para una capa en blanco: sin {@code JSON_METADATA}, origen ni categoría. */
    CapaEntity(int owner, String name, String description, String status, String visibility) {
        this(owner, name, description, status, visibility, null);
    }

    /** Una capa importada de un archivo: sus elementos van solo en {@code LAYERS_FEATURES}, como los dibujados. */
    CapaEntity(int owner, String name, String description, String status, String visibility, String origin) {
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.status = status;
        this.visibility = visibility;
        this.origin = origin;
    }

    /** Lo que guarda SIATE para una capa por URL: el servidor en {@code JSON_METADATA}, sin origen ni categoría. */
    static CapaEntity porUrl(int owner, String name, String description, String status, String visibility, String jsonMetadata) {
        CapaEntity capa = new CapaEntity(owner, name, description, status, visibility);
        capa.jsonMetadata = jsonMetadata;
        return capa;
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
