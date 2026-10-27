package siante2.capas.domain;

import java.util.List;

/** Capa para mostrar en el mapa. {@code propia}: el usuario es el dueño. Las capas por URL no traen elementos. */
public record Capa(int id, String nombre, String descripcion, Visibilidad visibilidad, boolean propia, TipoCapa tipo,
        List<ElementoCapa> elementos) {
}
