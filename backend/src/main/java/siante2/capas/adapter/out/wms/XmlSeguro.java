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

/** XML de un servidor de mapas, que es de un tercero: sin DOCTYPE ni entidades externas. Se lee en bytes para que el
 * parser tome la codificación de la declaración XML: muchos servidores no la informan en Content-Type. */
final class XmlSeguro {

    private XmlSeguro() {
    }

    /** Lanza {@code IllegalArgumentException} si está vacío o no es XML; {@code queEs} va en el mensaje. */
    static Element raiz(byte[] xml, String queEs) {
        if (xml == null || xml.length == 0) {
            throw new IllegalArgumentException(queEs + " vacío");
        }
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setExpandEntityReferences(false);
            return fabrica.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml))).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException(queEs + " no reconocido: " + e.getMessage(), e);
        }
    }

    static List<Element> hijos(Element padre) {
        List<Element> hijos = new ArrayList<>();
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                hijos.add(e);
            }
        }
        return hijos;
    }

    /** Texto del primer hijo directo con ese nombre, sin espacios; vacío si no está. */
    static String texto(Element padre, String nombre) {
        return hijos(padre).stream().filter(h -> nombre.equals(h.getLocalName())).findFirst()
                .map(h -> h.getTextContent().trim()).orElse("");
    }
}
