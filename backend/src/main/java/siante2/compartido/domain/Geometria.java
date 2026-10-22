package siante2.compartido.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Geometría GeoJSON (RFC 7946) en EPSG:4326, longitud primero. {@code coordinates} anida listas de
 * {@code Double} según el tipo; {@code geometries} solo se usa en GeometryCollection. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Geometria(String type, List<?> coordinates, List<Geometria> geometries) {

    public static final int SRID = 4326;

    public static Geometria simple(String type, List<?> coordinates) {
        return new Geometria(type, coordinates, null);
    }

    public static Geometria coleccion(List<Geometria> geometries) {
        return new Geometria("GeometryCollection", null, geometries);
    }
}
