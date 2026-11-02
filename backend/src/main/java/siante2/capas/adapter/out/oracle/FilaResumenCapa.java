package siante2.capas.adapter.out.oracle;

import java.util.Objects;

/** Proyección del listado: el adaptador traduce el código de visibilidad de SIATE al dominio. */
public record FilaResumenCapa(Integer id, String nombre, String descripcion, String visibilidad) {

    /** En Oracle {@code ''} es NULL: el {@code coalesce(x, '')} de la consulta no evita los null. */
    public FilaResumenCapa {
        nombre = Objects.requireNonNullElse(nombre, "");
        descripcion = Objects.requireNonNullElse(descripcion, "");
    }
}
