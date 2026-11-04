package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Los íconos de los marcadores: tabla {@code POINTS_CATALOG} de SIATE (modelo {@code PointsCatalog}). Sin {@code @Immutable}
 * porque se escribe, y sin setters: se da de alta y se cambia solo con {@link IconoJpaRepository}. */
@Entity
@Table(name = "POINTS_CATALOG")
public class PuntoCatalogoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String url;

    private String nameFile;

    private String visibility;

    private Integer owner;

    private String nameImage;

    protected PuntoCatalogoEntity() {
    }

    /** Como SIATE: la imagen en {@code NAME_FILE}, sin URL. */
    PuntoCatalogoEntity(int owner, String name, String nameImage, String nameFile, String visibility) {
        this.owner = owner;
        this.name = name;
        this.nameImage = nameImage;
        this.nameFile = nameFile;
        this.visibility = visibility;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getNameFile() {
        return nameFile;
    }

    public String getVisibility() {
        return visibility;
    }

    public Integer getOwner() {
        return owner;
    }

    public String getNameImage() {
        return nameImage;
    }
}
