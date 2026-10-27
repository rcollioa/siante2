package siante2.multimedia.adapter.out.oracle;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Archivo multimedia (manual, GIF de ayuda): tabla {@code MULTIMEDIA} de SIATE (modelo {@code Multimedia}), solo
 * lectura. {@code ARCHIVO} solo lo lee la consulta que lo pide; los listados usan proyecciones. */
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

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] archivo;

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
