package siante2.multimedia.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Archivo multimedia (manual de usuario): tabla {@code MULTIMEDIA} de SIANTE (modelo {@code Multimedia}), solo lectura. */
@Entity
@Immutable
@Table(name = "MULTIMEDIA")
public class MultimediaEntity {

    @Id
    @Column(name = "X_MULTIMEDIA")
    private Integer id;

    @Column(name = "CREADO")
    private String creadoPor;

    @Column(name = "F_CREACION")
    private LocalDateTime fechaCreacion;

    @Column(name = "MODIFICADO")
    private String modificadoPor;

    @Column(name = "F_MODIFICACION")
    private LocalDateTime fechaModificacion;

    @Column(name = "DESCRIPCION")
    private String descripcion;

    protected MultimediaEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
