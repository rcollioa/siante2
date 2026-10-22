package siante2.reportes.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Immutable;

/** Capa incluida en un reporte público de grupo: tabla {@code PUBLIC_REPORTS_DETAILS} de SIANTE (modelo {@code PublicReportsDetails}), solo lectura. */
@Entity
@Immutable
@Table(name = "PUBLIC_REPORTS_DETAILS")
public class DetalleReportePublicoEntity {

    @Id
    @Column(name = "X_PUREDET")
    private Integer id;

    @Column(name = "CREADO")
    private Integer creadoPor;

    @Column(name = "F_CREACION")
    private LocalDateTime fechaCreacion;

    @Column(name = "PURE_X_PURE")
    private Integer reporteId;

    @Column(name = "LAYER_ID")
    private Integer capaId;

    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "QR")
    private String qr;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "JSON_METADATA")
    private String metadatosJson;

    protected DetalleReportePublicoEntity() {
    }

    public Integer getId() {
        return id;
    }

    public Integer getCreadoPor() {
        return creadoPor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Integer getReporteId() {
        return reporteId;
    }

    public Integer getCapaId() {
        return capaId;
    }

    public Integer getVersion() {
        return version;
    }

    public String getQr() {
        return qr;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMetadatosJson() {
        return metadatosJson;
    }
}
