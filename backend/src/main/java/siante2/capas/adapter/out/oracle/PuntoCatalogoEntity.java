package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Catálogo de puntos para importar: tabla {@code POINTS_CATALOG} de SIANTE (modelo {@code PointsCatalog}), solo lectura. */
@Entity
@Immutable
@Table(name = "POINTS_CATALOG")
public class PuntoCatalogoEntity {

    @Id
    private Integer id;

    private String name;

    private String url;

    private String nameFile;

    private String visibility;

    private Integer owner;

    private String nameImage;

    protected PuntoCatalogoEntity() {
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
