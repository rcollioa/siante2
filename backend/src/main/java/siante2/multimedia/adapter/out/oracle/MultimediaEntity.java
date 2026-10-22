package siante2.multimedia.adapter.out.oracle;

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
    private Integer xMultimedia;

    private String creado;

    private LocalDateTime fCreacion;

    private String modificado;

    private LocalDateTime fModificacion;

    private String descripcion;

    protected MultimediaEntity() {
    }

    public Integer getXMultimedia() {
        return xMultimedia;
    }

    public String getCreado() {
        return creado;
    }

    public LocalDateTime getFCreacion() {
        return fCreacion;
    }

    public String getModificado() {
        return modificado;
    }

    public LocalDateTime getFModificacion() {
        return fModificacion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
