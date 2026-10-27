package siante2.capas.domain;

/** Una capa en un listado, sin sus elementos: los de una sola capa pueden pesar varios megas. */
public record ResumenCapa(int id, String nombre, String descripcion, Visibilidad visibilidad) {
}
