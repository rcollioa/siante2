package siante2.reportes.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Reporte público (QR) de una capa o grupo: tabla {@code PUBLIC_REPORTS} de SIANTE (modelo {@code PublicReports}), solo lectura. */
@Entity
@Immutable
@Table(name = "PUBLIC_REPORTS")
public class ReportePublicoEntity {

    @Id
    private Integer xPure;

    private Integer creado;

    private LocalDateTime fCreacion;

    private Integer version;

    private Integer layerId;

    private Integer layersGroupsId;

    private String qr;

    private String name;

    private String jsonMetadata;

    private String descripcion;

    protected ReportePublicoEntity() {
    }

    public Integer getXPure() {
        return xPure;
    }

    public Integer getCreado() {
        return creado;
    }

    public LocalDateTime getFCreacion() {
        return fCreacion;
    }

    public Integer getVersion() {
        return version;
    }

    public Integer getLayerId() {
        return layerId;
    }

    public Integer getLayersGroupsId() {
        return layersGroupsId;
    }

    public String getQr() {
        return qr;
    }

    public String getName() {
        return name;
    }

    public String getJsonMetadata() {
        return jsonMetadata;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
