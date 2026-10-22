package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Column;
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
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "URL")
    private String url;

    @Column(name = "NAME_FILE")
    private String nombreArchivo;

    @Column(name = "VISIBILITY")
    private String visibilidad;

    @Column(name = "OWNER")
    private Integer dueno;

    @Column(name = "NAME_IMAGE")
    private String nombreImagen;

    protected PuntoCatalogoEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUrl() {
        return url;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getVisibilidad() {
        return visibilidad;
    }

    public Integer getDueno() {
        return dueno;
    }

    public String getNombreImagen() {
        return nombreImagen;
    }
}
