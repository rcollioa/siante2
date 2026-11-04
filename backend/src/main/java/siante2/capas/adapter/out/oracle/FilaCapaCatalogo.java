package siante2.capas.adapter.out.oracle;

import java.util.Objects;

/** Proyección del catálogo: el adaptador traduce la visibilidad y arma el nombre del dueño. */
public record FilaCapaCatalogo(Integer id, String nombre, String descripcion, String visibilidad, String categoria,
                               String nombreDueno, String apellidoDueno, Boolean favorita) {

    /** En Oracle {@code ''} es NULL: el {@code coalesce(x, '')} de la consulta no evita los null. */
    public FilaCapaCatalogo {
        nombre = Objects.requireNonNullElse(nombre, "");
        descripcion = Objects.requireNonNullElse(descripcion, "");
        categoria = Objects.requireNonNullElse(categoria, "");
        nombreDueno = Objects.requireNonNullElse(nombreDueno, "");
        apellidoDueno = Objects.requireNonNullElse(apellidoDueno, "");
    }
}
