package siante2.reportes.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Vista {@code NEXTVAL_PUBLIC_REPORTS} de SIATE (modelo {@code NextValPublicReports}): cada lectura entrega el
 * siguiente correlativo de reportes públicos (X_PURE). */
@Entity
@Immutable
@Table(name = "NEXTVAL_PUBLIC_REPORTS")
public class CorrelativoReporteEntity {

    @Id
    private Integer correlativo;

    protected CorrelativoReporteEntity() {
    }

    public Integer getCorrelativo() {
        return correlativo;
    }
}
