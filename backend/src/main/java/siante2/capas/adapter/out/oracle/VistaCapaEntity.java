package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Vista de capas con su dueño y categoría: tabla {@code V_LAYERS} de SIANTE (modelo {@code VLayers}), solo lectura. */
@Entity
@Immutable
@Table(name = "V_LAYERS")
public class VistaCapaEntity {

    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "ORIGIN")
    private String origen;

    @Column(name = "JSON_METADATA")
    private String metadatosJson;

    @Column(name = "DESCRIPTION")
    private String descripcion;

    @Column(name = "VISIBILITY")
    private String visibilidad;

    @Column(name = "STATUS")
    private String estado;

    @Column(name = "USER_ID")
    private Integer usuarioId;

    @Column(name = "USER_NAME")
    private String usuarioNombre;

    @Column(name = "USER_LAST_NAME")
    private String usuarioApellido;

    @Column(name = "USER_EMAIL")
    private String usuarioEmail;

    @Column(name = "USER_AREA")
    private String usuarioArea;

    @Column(name = "CATEGORY_ID")
    private Integer categoriaId;

    @Column(name = "CATEGORY_NAME")
    private String categoriaNombre;

    protected VistaCapaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getOrigen() {
        return origen;
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

    public String getEstado() {
        return estado;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public String getUsuarioApellido() {
        return usuarioApellido;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public String getUsuarioArea() {
        return usuarioArea;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }
}
