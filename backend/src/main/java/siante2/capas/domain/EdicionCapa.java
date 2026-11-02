package siante2.capas.domain;

/** Los datos de una capa que su dueño puede cambiar: los del formulario "Editar capa" de SIATE. */
public record EdicionCapa(String nombre, String descripcion, int categoriaId, Visibilidad visibilidad) {
}
