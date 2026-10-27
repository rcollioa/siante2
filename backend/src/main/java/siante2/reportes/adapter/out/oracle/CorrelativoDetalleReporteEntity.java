package siante2.reportes.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Vista {@code NEXTVAL_PUBLIC_REPORTS_DETAILS} de SIATE (modelo {@code NextValPublicReportsDetails}): cada lectura entrega el
 * siguiente correlativo de detalles de reportes públicos (X_PUREDET). */
@Entity
@Immutable
@Table(name = "NEXTVAL_PUBLIC_REPORTS_DETAILS")
public class CorrelativoDetalleReporteEntity {

    @Id
    private Integer correlativo;

    protected CorrelativoDetalleReporteEntity() {
    }

    public Integer getCorrelativo() {
        return correlativo;
    }
}
