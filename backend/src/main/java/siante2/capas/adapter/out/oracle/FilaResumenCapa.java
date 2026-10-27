package siante2.capas.adapter.out.oracle;

/** Proyección del listado: el adaptador traduce el código de visibilidad de SIATE al dominio. */
public record FilaResumenCapa(Integer id, String nombre, String descripcion, String visibilidad) {
}
