package siante2.compartido.adapter.out.oracle;

import java.util.List;
import java.util.stream.Collectors;
import siante2.compartido.domain.Geometria;
import siante2.compartido.domain.SolicitudInvalidaException;

/** Valida una geometría GeoJSON del cliente y la escribe en WKT para guardarla con
 * {@link #SQL_ESCRIBIR}; los valores van siempre como parámetro, nunca concatenados en el SQL. */
public final class GeometriaAWkt {

    /** Expresión SQL para insertar o actualizar una columna SDO_GEOMETRY desde el WKT (parámetro {@code ?}). */
    public static final String SQL_ESCRIBIR = "SDO_UTIL.FROM_WKTGEOMETRY(?, " + Geometria.SRID + ")";

    /** Expresión SQL para leer una columna SDO_GEOMETRY como GML 2 (ver {@link GmlAGeometria}). */
    public static String sqlLeer(String columna) {
        return "SDO_UTIL.TO_GMLGEOMETRY(" + columna + ")";
    }

    private GeometriaAWkt() {
    }

    public static String escribir(Geometria g) {
        if (g == null || g.type() == null) {
            throw new SolicitudInvalidaException("La geometría es obligatoria.");
        }
        return switch (g.type()) {
            case "Point" -> "POINT (" + punto(g.coordinates()) + ")";
            case "LineString" -> "LINESTRING " + linea(g.coordinates(), 2);
            case "Polygon" -> "POLYGON " + poligono(g.coordinates());
            case "MultiPoint" -> "MULTIPOINT " + lista(g.coordinates(), p -> "(" + punto(p) + ")");
            case "MultiLineString" -> "MULTILINESTRING " + lista(g.coordinates(), l -> linea(l, 2));
            case "MultiPolygon" -> "MULTIPOLYGON " + lista(g.coordinates(), GeometriaAWkt::poligono);
            case "GeometryCollection" -> {
                if (g.geometries() == null || g.geometries().isEmpty()) {
                    throw new SolicitudInvalidaException("La colección de geometrías está vacía.");
                }
                yield "GEOMETRYCOLLECTION (" + g.geometries().stream().map(GeometriaAWkt::escribir)
                        .collect(Collectors.joining(", ")) + ")";
            }
            default -> throw new SolicitudInvalidaException("Tipo de geometría no soportado: " + g.type());
        };
    }

    /** Oracle (ORA-13367) y GeoJSON exigen exterior antihorario y huecos horarios; se corrige si viene al revés. */
    private static String poligono(Object anillos) {
        List<?> lista = comoLista(anillos);
        StringBuilder wkt = new StringBuilder("(");
        for (int i = 0; i < lista.size(); i++) {
            List<?> anillo = comoLista(lista.get(i));
            if (!anillo.getFirst().equals(anillo.getLast())) {
                throw new SolicitudInvalidaException("Los anillos de un polígono deben cerrarse.");
            }
            boolean antihorario = areaConSigno(anillo) > 0;
            if (antihorario != (i == 0)) {
                anillo = anillo.reversed();
            }
            wkt.append(i > 0 ? ", " : "").append(linea(anillo, 4));
        }
        return wkt.append(")").toString();
    }

    /** Fórmula del área de Gauss: positiva si el anillo va en sentido antihorario. */
    private static double areaConSigno(List<?> anillo) {
        double suma = 0;
        for (int i = 0; i + 1 < anillo.size(); i++) {
            List<?> a = comoLista(anillo.get(i));
            List<?> b = comoLista(anillo.get(i + 1));
            if (!(a.get(0) instanceof Number ax && a.get(1) instanceof Number ay
                    && b.get(0) instanceof Number bx && b.get(1) instanceof Number by)) {
                throw new SolicitudInvalidaException("Cada punto debe tener longitud y latitud numéricas.");
            }
            suma += ax.doubleValue() * by.doubleValue() - bx.doubleValue() * ay.doubleValue();
        }
        return suma / 2;
    }

    private static String linea(Object puntos, int minimo) {
        List<?> lista = comoLista(puntos);
        if (lista.size() < minimo) {
            throw new SolicitudInvalidaException("La geometría necesita al menos " + minimo + " puntos.");
        }
        return lista(lista, GeometriaAWkt::punto);
    }

    private static String lista(Object elementos, java.util.function.Function<Object, String> escribir) {
        return comoLista(elementos).stream().map(escribir).collect(Collectors.joining(", ", "(", ")"));
    }

    /** Longitud y latitud en EPSG:4326; una tercera coordenada (altura) se conserva. */
    private static String punto(Object coordenadas) {
        List<?> c = comoLista(coordenadas);
        if (c.size() < 2 || c.size() > 3 || !(c.stream().allMatch(v -> v instanceof Number))) {
            throw new SolicitudInvalidaException("Cada punto debe tener longitud y latitud numéricas.");
        }
        double lon = ((Number) c.get(0)).doubleValue();
        double lat = ((Number) c.get(1)).doubleValue();
        if (!Double.isFinite(lon) || !Double.isFinite(lat) || Math.abs(lon) > 180 || Math.abs(lat) > 90) {
            throw new SolicitudInvalidaException("Coordenada fuera de rango (EPSG:4326): " + lon + ", " + lat);
        }
        return c.stream().map(v -> Double.toString(((Number) v).doubleValue())).collect(Collectors.joining(" "));
    }

    private static List<?> comoLista(Object valor) {
        if (!(valor instanceof List<?> lista) || lista.isEmpty()) {
            throw new SolicitudInvalidaException("Coordenadas de la geometría mal formadas.");
        }
        return lista;
    }
}
