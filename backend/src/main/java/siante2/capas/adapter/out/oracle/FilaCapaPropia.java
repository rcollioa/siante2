package siante2.capas.adapter.out.oracle;

import java.util.Objects;

/** Proyección de "Mis capas": el adaptador traduce el código de visibilidad de SIATE al dominio. */
public record FilaCapaPropia(Integer id, String nombre, String descripcion, String visibilidad, Integer categoriaId) {

    /** En Oracle {@code ''} es NULL: el {@code coalesce(x, '')} de la consulta no evita los null. */
    public FilaCapaPropia {
        nombre = Objects.requireNonNullElse(nombre, "");
        descripcion = Objects.requireNonNullElse(descripcion, "");
    }
}
