package siante2.capas.adapter.out.oracle;

/** Proyección de una capa. {@code inicioJson}: los primeros caracteres de {@code JSON_METADATA}, que bastan para
 * saber si la capa es por URL sin leer el CLOB completo. */
public record FilaCapaVisible(Integer id, String nombre, String descripcion, String visibilidad, Integer dueno,
        String inicioJson) {
}
