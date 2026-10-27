package siante2.reportes.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Capa incluida en un reporte público de grupo: tabla {@code PUBLIC_REPORTS_DETAILS} de SIATE (modelo {@code PublicReportsDetails}), solo lectura. */
@Entity
@Immutable
@Table(name = "PUBLIC_REPORTS_DETAILS")
public class DetalleReportePublicoEntity {

    @Id
    private Integer xPuredet;

    private Integer creado;

    private LocalDateTime fCreacion;

    @Column(name = "PURE_X_PURE")
    private Integer pureXPure;

    private Integer layerId;

    private Integer version;

    private String qr;

    private String name;

    private String jsonMetadata;

    protected DetalleReportePublicoEntity() {
    }

    public Integer getXPuredet() {
        return xPuredet;
    }

    public Integer getCreado() {
        return creado;
    }

    public LocalDateTime getFCreacion() {
        return fCreacion;
    }

    public Integer getPureXPure() {
        return pureXPure;
    }

    public Integer getLayerId() {
        return layerId;
    }

    public Integer getVersion() {
        return version;
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
}
