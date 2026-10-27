package siante2.capas.domain;

import java.util.List;
import siante2.compartido.domain.Geometria;

/** Elemento dibujado o importado. GeoJSON no tiene círculos: en uno de SIATE la geometría es el centro (Point) y
 * {@code radio} va en unidades del mapa (EPSG:3857), como lo guarda SIATE. En los demás, {@code radio} es 0. */
public record ElementoCapa(int id, Geometria geometria, double radio, String nombre, String descripcion,
        EstiloElemento estilo, List<Metadato> metadatos) {
}
