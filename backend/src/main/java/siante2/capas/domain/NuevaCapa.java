package siante2.capas.domain;

/** Los "Datos básicos" de una capa en blanco, como en SIATE: su categoría y su visibilidad se eligen después, al editarla. */
public record NuevaCapa(String nombre, String descripcion) {
}
