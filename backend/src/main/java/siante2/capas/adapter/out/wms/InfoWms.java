package siante2.capas.adapter.out.wms;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.Metadato;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static siante2.capas.adapter.out.wms.XmlSeguro.hijos;
import static siante2.capas.adapter.out.wms.XmlSeguro.texto;

/** Lee la respuesta de un GetFeatureInfo. SIATE pedía JSON o, a los ArcGIS, su API REST; aquí se pide por WMS en el
 * mejor formato que el servidor declare: GeoJSON (GeoServer, ArcGIS 10.x), el XML de ArcGIS o GML (GeoServer,
 * MapServer). HTML y texto plano no se piden: no se pueden leer como datos. */
final class InfoWms {

    /** De más a menos preferido; se comparan en minúsculas. */
    private static final List<String> FORMATOS = List.of("application/geo+json", "application/geojson",
            "application/json", "application/vnd.geo+json", "application/vnd.esri.wms_featureinfo_xml", "text/xml",
            "application/vnd.ogc.wms_xml", "application/vnd.ogc.gml");
    private static final String GML = "http://www.opengis.net/gml";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private InfoWms() {
    }

    /** El formato que se pide al servidor, tal como él lo escribe; vacío si no ofrece ninguno que se sepa leer. */
    static Optional<String> formatoPreferido(List<String> ofrecidos) {
        for (String preferido : FORMATOS) {
            Optional<String> ofrecido = ofrecidos.stream()
                    .filter(f -> f.toLowerCase(Locale.ROOT).equals(preferido)).findFirst();
            if (ofrecido.isPresent()) {
                return ofrecido;
            }
        }
        return Optional.empty();
    }

    /** Lanza {@code IllegalArgumentException} si la respuesta no es lo que se pidió (p. ej. un error en HTML). */
    static List<ElementoDeServicio> leer(byte[] cuerpo, String formato) {
        return formato.toLowerCase(Locale.ROOT).contains("json") ? deJson(cuerpo) : deXml(cuerpo);
    }

    /** GeoJSON: las propiedades de cada elemento, sin la geometría. La capa, de {@code layerName} (ArcGIS) o del
     * prefijo del {@code id} (GeoServer: "rios.12"). */
    private static List<ElementoDeServicio> deJson(byte[] cuerpo) {
        JsonNode raiz;
        try {
            raiz = JSON.readTree(new String(cuerpo, StandardCharsets.UTF_8));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("GetFeatureInfo no es JSON: " + e.getOriginalMessage(), e);
        }
        List<ElementoDeServicio> elementos = new ArrayList<>();
        for (JsonNode elemento : raiz.path("features")) {
            List<Metadato> datos = new ArrayList<>();
            elemento.path("properties").properties().forEach(propiedad -> {
                JsonNode valor = propiedad.getValue();
                if (valor.isValueNode()) {
                    datos.add(new Metadato(propiedad.getKey(), valor.isNull() ? "" : valor.asString()));
                }
            });
            String capa = elemento.path("layerName").asString("");
            String id = elemento.path("id").asString("");
            if (capa.isEmpty() && id.contains(".")) {
                capa = id.substring(0, id.lastIndexOf('.'));
            }
            elementos.add(new ElementoDeServicio(capa, datos));
        }
        return elementos;
    }

    private static List<ElementoDeServicio> deXml(byte[] cuerpo) {
        Element raiz = XmlSeguro.raiz(cuerpo, "GetFeatureInfo");
        if ("ServiceExceptionReport".equals(raiz.getLocalName())) {
            throw new IllegalArgumentException("GetFeatureInfo con error: " + raiz.getTextContent().trim());
        }
        List<ElementoDeServicio> elementos = new ArrayList<>();
        recorrer(raiz, elementos);
        return elementos;
    }

    private static void recorrer(Element padre, List<ElementoDeServicio> elementos) {
        for (Element hijo : hijos(padre)) {
            String nombre = hijo.getLocalName();
            if ("FeatureInfo".equals(nombre)) {
                elementos.add(arcGis(hijo, padre.getAttribute("layername")));
            } else if ("FIELDS".equals(nombre)) {
                elementos.add(new ElementoDeServicio(padre.getAttribute("layername"), atributos(hijo)));
            } else if (nombre.endsWith("_feature")) {
                // MapServer: <rios_layer><rios_feature><NOMBRE>…</NOMBRE></rios_feature></rios_layer>
                elementos.add(new ElementoDeServicio(nombre.substring(0, nombre.length() - "_feature".length()),
                        hojasDe(hijo)));
            } else if ("featureMember".equals(padre.getLocalName()) || "featureMembers".equals(padre.getLocalName())) {
                // GeoServer: <gml:featureMember><ide:rios><ide:NOMBRE>…</ide:NOMBRE></ide:rios></gml:featureMember>
                elementos.add(new ElementoDeServicio(hijo.getLocalName(), hojasDe(hijo)));
            } else {
                recorrer(hijo, elementos);
            }
        }
    }

    /** ArcGIS 10.x: {@code <FeatureInfo><Fields><Field><FieldName/><FieldValue/></Field>}. */
    private static ElementoDeServicio arcGis(Element elemento, String capa) {
        List<Metadato> datos = new ArrayList<>();
        hijos(elemento).stream().filter(nombre("Fields")).flatMap(campos -> hijos(campos).stream())
                .filter(nombre("Field"))
                .forEach(campo -> datos.add(new Metadato(texto(campo, "FieldName"), texto(campo, "FieldValue"))));
        return new ElementoDeServicio(capa, datos);
    }

    /** ArcGIS antiguo: {@code <FIELDS OBJECTID="1" NOMBRE="…"/>}. */
    private static List<Metadato> atributos(Element elemento) {
        List<Metadato> datos = new ArrayList<>();
        NamedNodeMap atributos = elemento.getAttributes();
        for (int i = 0; i < atributos.getLength(); i++) {
            Attr atributo = (Attr) atributos.item(i);
            datos.add(new Metadato(atributo.getName(), atributo.getValue()));
        }
        return datos;
    }

    /** Los hijos con solo texto: la geometría (con elementos dentro) y lo propio de GML quedan fuera. */
    private static List<Metadato> hojasDe(Element elemento) {
        return hijos(elemento).stream()
                .filter(hijo -> !GML.equals(hijo.getNamespaceURI()) && hijos(hijo).isEmpty())
                .map(hijo -> new Metadato(hijo.getLocalName(), hijo.getTextContent().trim()))
                .toList();
    }

    private static Predicate<Element> nombre(String nombre) {
        return elemento -> nombre.equals(elemento.getLocalName());
    }
}
