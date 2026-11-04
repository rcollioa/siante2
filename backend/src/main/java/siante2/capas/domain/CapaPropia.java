package siante2.capas.domain;

/** Una capa del propio usuario en "Mis capas": como {@link ResumenCapa}, más la categoría que se edita con ella. Sin
 * categoría es {@code 0}, nunca null. */
public record CapaPropia(int id, String nombre, String descripcion, Visibilidad visibilidad, int categoriaId) {
}
