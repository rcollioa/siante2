package siante2.capas.adapter.out.oracle;

/** Una fila de {@code POINTS_CATALOG}; la imagen puede faltar en los íconos por URL de SIATE. */
record FilaIcono(int id, String nombre, String archivo, String imagen, int dueno) {
}
