package siante2.capas.domain;

import java.util.List;

/** Capa para mostrar en el mapa. {@code propia}: el usuario es el dueño. {@code editable}: además se puede dibujar en
 * ella, porque no es formal ni un área de dibujo, y sus elementos no vienen de una URL. Las capas por URL no traen
 * elementos sino {@code servicio}, el servidor de mapas que las dibuja; en las vectoriales es null. */
public record Capa(int id, String nombre, String descripcion, Visibilidad visibilidad, boolean propia, boolean editable,
        TipoCapa tipo, List<ElementoCapa> elementos, ServicioMapa servicio) {

    public Capa conServicio(ServicioMapa servicio) {
        return new Capa(id, nombre, descripcion, visibilidad, propia, editable, tipo, elementos, servicio);
    }
}
