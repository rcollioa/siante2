package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Column;
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
    @Column(name = "ID")
    private Integer id;

    @Column(name = "OWNER")
    private Integer dueno;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "ORIGIN")
    private String origen;

    @Column(name = "STATUS")
    private String estado;

    @Column(name = "JSON_METADATA")
    private String metadatosJson;

    @Column(name = "DESCRIPTION")
    private String descripcion;

    @Column(name = "VISIBILITY")
    private String visibilidad;

    @Column(name = "L_GROUP_ID")
    private Integer grupoId;

    @Column(name = "CATEGORY_ID")
    private Integer categoriaId;

    protected CapaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getDueno() {
        return dueno;
    }

    public String getNombre() {
        return nombre;
    }

    public String getOrigen() {
        return origen;
    }

    public String getEstado() {
        return estado;
    }

    public String getMetadatosJson() {
        return metadatosJson;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getVisibilidad() {
        return visibilidad;
    }

    public Integer getGrupoId() {
        return grupoId;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }
}
