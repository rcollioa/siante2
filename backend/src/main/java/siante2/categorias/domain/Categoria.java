package siante2.categorias.domain;

/** Categoría temática con la que se clasifican las capas. Sin descripción es {@code ""}, nunca null. */
public record Categoria(int id, String nombre, String descripcion) {
}
