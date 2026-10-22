package siante2.compartido.adapter.out.oracle;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import siante2.compartido.domain.Geometria;

/** Convierte el GML 2 de {@code SDO_UTIL.TO_GMLGEOMETRY} en GeoJSON. Con datos reales de Oracle de la CGR
 * fallan Hibernate Spatial, JGeometry y TO_WKTGEOMETRY (ORA-13000); TO_GMLGEOMETRY no. */
public final class GmlAGeometria {

    private GmlAGeometria() {
    }

    public static Geometria leer(String gml) {
        if (gml == null || gml.isBlank()) {
            return null;
        }
        try {
            return geometria(raiz(gml));
        } catch (ParserConfigurationException | SAXException | IOException | RuntimeException e) {
            throw new IllegalArgumentException("GML de Oracle no reconocido: " + e.getMessage(), e);
        }
    }

    private static Element raiz(String gml) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setExpandEntityReferences(false);
        return fabrica.newDocumentBuilder().parse(new InputSource(new StringReader(gml))).getDocumentElement();
    }

    private static Geometria geometria(Element e) {
        return switch (e.getLocalName()) {
            case "Point" -> Geometria.simple("Point", posiciones(e).getFirst());
            case "LineString" -> Geometria.simple("LineString", posiciones(e));
            case "Polygon" -> Geometria.simple("Polygon", anillos(e));
            case "MultiPoint" -> Geometria.simple("MultiPoint", miembros(e).stream().map(m -> posiciones(m).getFirst()).toList());
            case "MultiLineString" -> Geometria.simple("MultiLineString", miembros(e).stream().map(GmlAGeometria::posiciones).toList());
            case "MultiPolygon" -> Geometria.simple("MultiPolygon", miembros(e).stream().map(GmlAGeometria::anillos).toList());
            case "MultiGeometry" -> Geometria.coleccion(miembros(e).stream().map(GmlAGeometria::geometria).toList());
            default -> throw new IllegalArgumentException("tipo " + e.getLocalName());
        };
    }

    /** Hijos de pointMember, polygonMember, geometryMember, etc. */
    private static List<Element> miembros(Element multi) {
        List<Element> resultado = new ArrayList<>();
        for (Element miembro : hijos(multi)) {
            hijos(miembro).forEach(resultado::add);
        }
        return resultado;
    }

    /** Exterior primero, luego los huecos, como pide GeoJSON. */
    private static List<List<List<Double>>> anillos(Element poligono) {
        List<List<List<Double>>> resultado = new ArrayList<>();
        for (String borde : List.of("outerBoundaryIs", "exterior", "innerBoundaryIs", "interior")) {
            for (Element b : hijos(poligono)) {
                if (borde.equals(b.getLocalName())) {
                    resultado.add(posiciones(b));
                }
            }
        }
        return resultado;
    }

    /** Lee gml:coordinates (GML 2, con sus separadores cs/ts/decimal) o gml:posList/gml:pos (GML 3). */
    private static List<List<Double>> posiciones(Element e) {
        Element coordenadas = buscar(e, "coordinates");
        if (coordenadas != null) {
            String cs = atributo(coordenadas, "cs", ",");
            String ts = atributo(coordenadas, "ts", " ");
            String decimal = atributo(coordenadas, "decimal", ".");
            List<List<Double>> puntos = new ArrayList<>();
            for (String tupla : coordenadas.getTextContent().trim().split(Pattern.quote(ts) + "+")) {
                if (tupla.isBlank()) {
                    continue;
                }
                List<Double> punto = new ArrayList<>();
                for (String valor : tupla.trim().split(Pattern.quote(cs))) {
                    punto.add(Double.parseDouble(valor.trim().replace(decimal, ".")));
                }
                puntos.add(punto);
            }
            return puntos;
        }
        Element lista = buscar(e, "posList");
        if (lista == null) {
            lista = buscar(e, "pos");
        }
        if (lista == null) {
            throw new IllegalArgumentException("sin coordenadas en " + e.getLocalName());
        }
        int dimension = Integer.parseInt(atributo(lista, "srsDimension", "2"));
        String[] valores = lista.getTextContent().trim().split("\\s+");
        List<List<Double>> puntos = new ArrayList<>();
        for (int i = 0; i + dimension <= valores.length; i += dimension) {
            List<Double> punto = new ArrayList<>();
            for (int d = 0; d < dimension; d++) {
                punto.add(Double.parseDouble(valores[i + d]));
            }
            puntos.add(punto);
        }
        return puntos;
    }

    private static Element buscar(Element e, String nombre) {
        if (nombre.equals(e.getLocalName())) {
            return e;
        }
        for (Element h : hijos(e)) {
            Element encontrado = buscar(h, nombre);
            if (encontrado != null) {
                return encontrado;
            }
        }
        return null;
    }

    private static List<Element> hijos(Element e) {
        List<Element> resultado = new ArrayList<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element h) {
                resultado.add(h);
            }
        }
        return resultado;
    }

    private static String atributo(Element e, String nombre, String porDefecto) {
        String valor = e.getAttribute(nombre);
        return valor.isEmpty() ? porDefecto : valor;
    }
}
