package siante2.reportes.adapter.out.oracle;

import jakarta.persistence.Column;
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
    @Column(name = "X_PURE")
    private Integer id;

    @Column(name = "CREADO")
    private Integer creadoPor;

    @Column(name = "F_CREACION")
    private LocalDateTime fechaCreacion;

    @Column(name = "VERSION")
    private Integer version;

    @Column(name = "LAYER_ID")
    private Integer capaId;

    @Column(name = "LAYERS_GROUPS_ID")
    private Integer grupoId;

    @Column(name = "QR")
    private String qr;

    @Column(name = "NAME")
    private String nombre;

    @Column(name = "JSON_METADATA")
    private String metadatosJson;

    @Column(name = "DESCRIPCION")
    private String descripcion;

    protected ReportePublicoEntity() {
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

    public Integer getVersion() {
        return version;
    }

    public Integer getCapaId() {
        return capaId;
    }

    public Integer getGrupoId() {
        return grupoId;
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

    public String getDescripcion() {
        return descripcion;
    }
}
