package siante2.capas.adapter.out.oracle;

/** Proyección del catálogo: el adaptador traduce la visibilidad y arma el nombre del dueño. */
public record FilaCapaCatalogo(Integer id, String nombre, String descripcion, String visibilidad, String categoria,
                               String nombreDueno, String apellidoDueno, Boolean favorita) {
}
