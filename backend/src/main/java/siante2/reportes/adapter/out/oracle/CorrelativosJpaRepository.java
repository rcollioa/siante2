package siante2.reportes.adapter.out.oracle;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Correlativos para dar de alta reportes públicos, como en {@code PublicReportController} de SIATE. */
interface CorrelativosJpaRepository extends Repository<CorrelativoReporteEntity, Integer> {

    @Query("select c.correlativo from CorrelativoReporteEntity c")
    Integer siguienteReporte();

    @Query("select c.correlativo from CorrelativoDetalleReporteEntity c")
    Integer siguienteDetalleReporte();
}
