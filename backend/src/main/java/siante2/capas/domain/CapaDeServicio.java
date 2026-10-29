package siante2.capas.domain;

/** Una capa que publica un servidor de mapas. {@code extension}: null si el servidor no la informa. */
public record CapaDeServicio(String nombre, String titulo, Extension extension) {
}
