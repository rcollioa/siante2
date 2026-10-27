package siante2.capas.adapter.out.oracle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Capas que se pueden elegir en la selección por área: vista {@code V_SELECT_AREA} de SIATE (modelo
 * {@code VSelectArea}), solo lectura. Clave inferida (la vista no tiene clave): leer solo con proyecciones. */
@Entity
@Immutable
@Table(name = "V_SELECT_AREA")
public class VistaCapaSeleccionableEntity {

    @Id
    private Integer id;

    private String name;

    private Integer owner;

    private Integer visibility;

    protected VistaCapaSeleccionableEntity() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getOwner() {
        return owner;
    }

    public Integer getVisibility() {
        return visibility;
    }
}
