package siante2.direcciones.domain;

/** Centro (EPSG:4326) y zoom del mapa que se está mirando: las sugerencias cercanas aparecen primero. */
public record ZonaVisible(double latitud, double longitud, int zoom) {
}
