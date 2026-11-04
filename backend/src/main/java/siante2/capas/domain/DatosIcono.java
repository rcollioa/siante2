package siante2.capas.domain;

/** Lo que el usuario define de un ícono: su nombre, el nombre del archivo original y la imagen como URL {@code data:}. */
public record DatosIcono(String nombre, String archivo, String imagen) {
}
