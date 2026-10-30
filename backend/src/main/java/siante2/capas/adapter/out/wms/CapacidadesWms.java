package siante2.capas.adapter.out.wms;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.Extension;

import static siante2.capas.adapter.out.wms.XmlSeguro.hijos;
import static siante2.capas.adapter.out.wms.XmlSeguro.texto;

/** Lee las capas de un GetCapabilities WMS 1.1.1 o 1.3.0. Como SIATE, las capas con nombre que no agrupan otras;
 * si el servidor solo publica grupos, las capas con nombre que haya. La extensión se hereda del grupo si la capa no
 * la trae. */
final class CapacidadesWms {

    private CapacidadesWms() {
    }

    /** Las capas publicadas y los formatos en que el servidor responde GetFeatureInfo. */
    record Leidas(List<CapaDeServicio> capas, List<String> formatosInfo) {
    }

    static Leidas leer(byte[] xml) {
        Element raiz = XmlSeguro.raiz(xml, "GetCapabilities WMS");
        List<CapaDeServicio> hojas = new ArrayList<>();
        List<CapaDeServicio> conNombre = new ArrayList<>();
        recorrer(raiz, null, hojas, conNombre);
        return new Leidas(hojas.isEmpty() ? conNombre : hojas, formatosInfo(raiz));
    }

    /** Capability/Request/GetFeatureInfo/Format, igual en 1.1.1 y 1.3.0. */
    private static List<String> formatosInfo(Element raiz) {
        return hijos(raiz).stream().filter(h -> "Capability".equals(h.getLocalName()))
                .flatMap(capacidad -> hijos(capacidad).stream()).filter(h -> "Request".equals(h.getLocalName()))
                .flatMap(pedidos -> hijos(pedidos).stream()).filter(h -> "GetFeatureInfo".equals(h.getLocalName()))
                .flatMap(info -> hijos(info).stream()).filter(h -> "Format".equals(h.getLocalName()))
                .map(formato -> formato.getTextContent().trim())
                .toList();
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
}
