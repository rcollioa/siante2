package siante2.capas.domain;

/** Rectángulo en grados (EPSG:4326) que cubre una capa, para acercar el mapa a ella. */
public record Extension(double oeste, double sur, double este, double norte) {
}
