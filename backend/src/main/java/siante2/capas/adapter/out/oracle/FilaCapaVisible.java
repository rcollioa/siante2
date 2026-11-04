package siante2.capas.adapter.out.oracle;

import java.util.Objects;

/** Proyección de una capa. {@code inicioJson}: los primeros caracteres de {@code JSON_METADATA}, que bastan para
 * saber si la capa es por URL sin leer el CLOB completo. */
public record FilaCapaVisible(Integer id, String nombre, String descripcion, String visibilidad, Integer dueno,
        String inicioJson) {

    /** En Oracle {@code ''} es NULL: el {@code coalesce(x, '')} de la consulta no evita los null. */
    public FilaCapaVisible {
        nombre = Objects.requireNonNullElse(nombre, "");
        descripcion = Objects.requireNonNullElse(descripcion, "");
    }
}
