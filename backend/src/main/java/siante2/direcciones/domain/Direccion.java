package siante2.direcciones.domain;

/** Resultado de geocodificar un texto: nombre completo y punto en EPSG:4326. */
public record Direccion(String nombre, double latitud, double longitud) {
}
