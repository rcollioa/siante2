package siante2.capas.adapter.out.wms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.Extension;

/** Lee las capas de un GetCapabilities WMS 1.1.1 o 1.3.0. Como SIATE, las capas con nombre que no agrupan otras;
 * si el servidor solo publica grupos, las capas con nombre que haya. La extensión se hereda del grupo si la capa no
 * la trae. XML de un tercero: sin DOCTYPE ni entidades externas. Se lee en bytes para que el parser tome la
 * codificación de la declaración XML: muchos servidores no la informan en Content-Type. */
final class CapacidadesWms {

    private CapacidadesWms() {
    }

    static List<CapaDeServicio> leer(byte[] xml) {
        List<CapaDeServicio> hojas = new ArrayList<>();
        List<CapaDeServicio> conNombre = new ArrayList<>();
        recorrer(raiz(xml), null, hojas, conNombre);
        return hojas.isEmpty() ? conNombre : hojas;
    }

    private static void recorrer(Element elemento, Extension heredada, List<CapaDeServicio> hojas,
                                 List<CapaDeServicio> conNombre) {
        for (Element hijo : hijos(elemento)) {
            if (!"Layer".equals(hijo.getLocalName())) {
                recorrer(hijo, heredada, hojas, conNombre);
                continue;
            }
            Extension propia = extension(hijo);
            Extension vigente = propia != null ? propia : heredada;
            String nombre = texto(hijo, "Name");
            if (!nombre.isEmpty()) {
                String titulo = texto(hijo, "Title");
                CapaDeServicio capa = new CapaDeServicio(nombre, titulo.isEmpty() ? nombre : titulo, vigente);
                conNombre.add(capa);
                if (hijos(hijo).stream().noneMatch(n -> "Layer".equals(n.getLocalName()))) {
                    hojas.add(capa);
                }
            }
            recorrer(hijo, vigente, hojas, conNombre);
        }
    }

    /** WMS 1.3.0: EX_GeographicBoundingBox; 1.1.1: LatLonBoundingBox. */
    private static Extension extension(Element capa) {
        for (Element hijo : hijos(capa)) {
            if ("EX_GeographicBoundingBox".equals(hijo.getLocalName())) {
                return numeros(texto(hijo, "westBoundLongitude"), texto(hijo, "southBoundLatitude"),
                        texto(hijo, "eastBoundLongitude"), texto(hijo, "northBoundLatitude"));
            }
            if ("LatLonBoundingBox".equals(hijo.getLocalName())) {
                return numeros(hijo.getAttribute("minx"), hijo.getAttribute("miny"), hijo.getAttribute("maxx"),
                        hijo.getAttribute("maxy"));
            }
        }
        return null;
    }

    private static Extension numeros(String oeste, String sur, String este, String norte) {
        try {
            return new Extension(Double.parseDouble(oeste), Double.parseDouble(sur), Double.parseDouble(este),
                    Double.parseDouble(norte));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Texto del primer hijo directo con ese nombre, sin espacios; vacío si no está. */
    private static String texto(Element padre, String nombre) {
        return hijos(padre).stream().filter(h -> nombre.equals(h.getLocalName())).findFirst()
                .map(h -> h.getTextContent().trim()).orElse("");
    }

    private static List<Element> hijos(Element padre) {
        List<Element> hijos = new ArrayList<>();
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                hijos.add(e);
            }
        }
        return hijos;
    }

    private static Element raiz(byte[] xml) {
        if (xml == null || xml.length == 0) {
            throw new IllegalArgumentException("GetCapabilities WMS vacío");
        }
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setExpandEntityReferences(false);
            return fabrica.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml))).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("GetCapabilities WMS no reconocido: " + e.getMessage(), e);
        }
    }
}
