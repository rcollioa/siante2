package siante2;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/** Cuenta las sentencias SQL que ejecuta Hibernate, para fijar en los tests que no haya N+1. */
public final class ContadorSql {

    private final Statistics estadisticas;

    public ContadorSql(EntityManagerFactory fabrica) {
        this.estadisticas = fabrica.unwrap(SessionFactory.class).getStatistics();
        this.estadisticas.clear();
    }

    public long sentencias() {
        return estadisticas.getPrepareStatementCount();
    }
}
