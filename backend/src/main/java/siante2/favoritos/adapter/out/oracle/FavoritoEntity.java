package siante2.favoritos.adapter.out.oracle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Immutable;

/** Favorito de un usuario: tabla {@code FAVORITES} de SIANTE (modelo {@code Favourites}), solo lectura. */
@Entity
@Immutable
@Table(name = "FAVORITES")
@IdClass(FavoritoEntity.Clave.class)
public class FavoritoEntity {

    @Id
    @Column(name = "USER_ID")
    private Integer usuarioId;

    @Id
    @Column(name = "L_GROUP_ID")
    private Integer grupoId;

    @Column(name = "CREATED_DATE")
    private LocalDateTime fechaCreacion;

    protected FavoritoEntity() {
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public Integer getGrupoId() {
        return grupoId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    /** Clave compuesta: la tabla de SIANTE no tiene ID propio. */
    public static class Clave implements Serializable {

        private Integer usuarioId;

        private Integer grupoId;

        protected Clave() {
        }

        public Clave(Integer usuarioId, Integer grupoId) {
            this.usuarioId = usuarioId;
            this.grupoId = grupoId;
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c
                    && Objects.equals(usuarioId, c.usuarioId)
                    && Objects.equals(grupoId, c.grupoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioId, grupoId);
        }
    }
}
