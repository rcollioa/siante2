package siante2.capas.domain;

/** Un ícono para los marcadores: tabla {@code POINTS_CATALOG} de SIATE. La imagen es una URL {@code data:}, que el elemento
 * copia al usarla. {@code propio} distingue los del usuario, que puede cambiar, de los del sistema. */
public record Icono(int id, String nombre, String archivo, String imagen, boolean propio) {
}
