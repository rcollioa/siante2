package siante2.capas.domain;

import java.util.List;
import java.util.Optional;

/** La grilla de mosaicos (TileMatrixSet) con que se dibujan las capas de un servidor WMTS, de menos a más zoom.
 * {@code crs}: EPSG:3857 (la del mapa) o EPSG:4326. */
public record GrillaWmts(String crs, List<MatrizMosaicos> matrices) {

    public GrillaWmts {
        matrices = List.copyOf(matrices);
    }

    public Optional<MatrizMosaicos> matriz(String id) {
        return matrices.stream().filter(m -> m.id().equals(id)).findFirst();
    }
}
