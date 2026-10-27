package siante2.favoritos.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Favorito de un usuario: tabla {@code FAVORITES} de SIATE (modelo {@code Favourites}), solo lectura. */
@Entity
@Immutable
@Table(name = "FAVORITES")
@IdClass(FavoritoEntity.Clave.class)
public class FavoritoEntity {

    @Id
    private Integer userId;

    @Id
    private Integer lGroupId;

    private LocalDateTime createdDate;

    protected FavoritoEntity() {
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getLGroupId() {
        return lGroupId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /** Clave compuesta: la tabla de SIATE no tiene ID propio. */
    public static class Clave implements Serializable {

        private Integer userId;

        private Integer lGroupId;

        protected Clave() {
        }

        public Clave(Integer userId, Integer lGroupId) {
            this.userId = userId;
            this.lGroupId = lGroupId;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c
                    && Objects.equals(userId, c.userId)
                    && Objects.equals(lGroupId, c.lGroupId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, lGroupId);
        }
    }
}
