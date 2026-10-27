package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Vista de capas con su dueño y categoría: tabla {@code V_LAYERS} de SIATE (modelo {@code VLayers}), solo lectura. */
@Entity
@Immutable
@Table(name = "V_LAYERS")
public class VistaCapaEntity {

    @Id
    private Integer id;

    private String name;

    private String origin;

    private String jsonMetadata;

    private String description;

    private String visibility;

    private String status;

    private Integer userId;

    private String userName;

    private String userLastName;

    private String userEmail;

    private String userArea;

    private Integer categoryId;

    private String categoryName;

    protected VistaCapaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOrigin() {
        return origin;
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

    public String getStatus() {
        return status;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserArea() {
        return userArea;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
