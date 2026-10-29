package siante2.capas.domain;

/** Una capa en el catálogo: lo necesario para buscarla y elegirla, sin sus elementos. */
public record CapaCatalogo(int id, String nombre, String descripcion, Visibilidad visibilidad, String categoria,
                           String dueno, boolean favorita) {
}
