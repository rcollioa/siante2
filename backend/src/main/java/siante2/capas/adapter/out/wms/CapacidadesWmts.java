package siante2.capas.adapter.out.wms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.w3c.dom.Element;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.Extension;
import siante2.capas.domain.GrillaWmts;
import siante2.capas.domain.MatrizMosaicos;

import static siante2.capas.adapter.out.wms.XmlSeguro.hijos;
import static siante2.capas.adapter.out.wms.XmlSeguro.texto;

/** Lee un GetCapabilities WMTS 1.0.0. SIATE tomaba el primer TileMatrixSet y lo leía OpenLayers en el navegador;
 * aquí se elige uno que el mapa sepa dibujar (EPSG:3857, el del mapa, o EPSG:4326, que OpenLayers reproyecta) y se
 * entrega ya calculado: el navegador no ve el XML ni la URL del servidor. */
final class CapacidadesWmts {

    /** Metros por grado en el ecuador: así WMTS pasa de escala a resolución en EPSG:4326. */
    private static final double METROS_POR_GRADO = 6378137 * 2 * Math.PI / 360;
    /** Tamaño de píxel de referencia de WMTS (0,28 mm). */
    private static final double PIXEL_METROS = 0.00028;
    private static final List<String> FORMATOS = List.of("image/png", "image/jpeg");

    private CapacidadesWmts() {
    }

    /** Lo que se necesita para dibujar una capa y pedir sus mosaicos. {@code plantilla}: la URL REST de sus
     * mosaicos (ResourceURL), o vacía si se piden por KVP. */
    record Capa(String id, String titulo, Extension extension, String estilo, String formato, List<String> grillas,
                String plantilla) {
    }

    /** {@code crs}: EPSG:3857 o EPSG:4326; vacío si el mapa no sabe dibujarla. */
    record Grilla(String id, String crs, List<MatrizMosaicos> matrices) {
    }

    /** {@code urlGetTile}: donde se piden los mosaicos por KVP; vacía si el servidor no lo dice. */
    record Leidas(List<Capa> capas, List<Grilla> grillas, String urlGetTile) {

        /** La grilla con que se dibuja: la primera en EPSG:3857 que use alguna capa; si no, la primera en 4326. */
        Optional<Grilla> grillaDelMapa() {
            return grillas.stream()
                    .filter(g -> !g.crs().isEmpty() && capas.stream().anyMatch(c -> c.grillas().contains(g.id())))
                    .min(Comparator.comparing(g -> !"EPSG:3857".equals(g.crs())));
        }

        /** Las capas que se dibujan con esa grilla. */
        List<Capa> capasDe(Grilla grilla) {
            return capas.stream().filter(c -> c.grillas().contains(grilla.id())).toList();
        }

        Optional<Capa> capa(String id) {
            return capas.stream().filter(c -> c.id().equals(id)).findFirst();
        }

        GrillaWmts grillaWmts(Grilla grilla) {
            return new GrillaWmts(grilla.crs(), grilla.matrices());
        }

        List<CapaDeServicio> capasDeServicio(Grilla grilla) {
            return capasDe(grilla).stream().map(c -> new CapaDeServicio(c.id(), c.titulo(), c.extension())).toList();
        }
    }

    static Leidas leer(byte[] xml) {
        Element raiz = XmlSeguro.raiz(xml, "GetCapabilities WMTS");
        List<Capa> capas = new ArrayList<>();
        List<Grilla> grillas = new ArrayList<>();
        for (Element contenido : hijosLlamados(raiz, "Contents")) {
            hijosLlamados(contenido, "Layer").forEach(capa -> capas.add(capa(capa)));
            hijosLlamados(contenido, "TileMatrixSet").forEach(grilla -> grillas.add(grilla(grilla)));
        }
        if (capas.isEmpty() && grillas.isEmpty()) {
            throw new IllegalArgumentException("GetCapabilities WMTS sin Contents");
        }
        return new Leidas(capas, grillas, urlGetTile(raiz));
    }

    private static Capa capa(Element capa) {
        String id = texto(capa, "Identifier");
        String titulo = texto(capa, "Title");
        List<String> formatos = hijosLlamados(capa, "Format").stream().map(f -> f.getTextContent().trim()).toList();
        String formato = FORMATOS.stream().filter(formatos::contains).findFirst()
                .orElse(formatos.isEmpty() ? "image/png" : formatos.getFirst());
        List<String> grillas = hijosLlamados(capa, "TileMatrixSetLink").stream()
                .map(enlace -> texto(enlace, "TileMatrixSet")).toList();
        List<Element> plantillas = hijosLlamados(capa, "ResourceURL").stream()
                .filter(r -> "tile".equalsIgnoreCase(r.getAttribute("resourceType"))).toList();
        String plantilla = plantillas.stream().filter(r -> formato.equals(r.getAttribute("format"))).findFirst()
                .or(() -> plantillas.stream().findFirst()).map(r -> r.getAttribute("template")).orElse("");
        return new Capa(id, titulo.isEmpty() ? id : titulo, extension(capa), estilo(capa), formato, grillas, plantilla);
    }

    /** El estilo por omisión; si ninguno lo es, el primero; sin estilos, "default" (el nombre que usa la norma). */
    private static String estilo(Element capa) {
        List<Element> estilos = hijosLlamados(capa, "Style");
        return estilos.stream().filter(e -> "true".equalsIgnoreCase(e.getAttribute("isDefault"))).findFirst()
                .or(() -> estilos.stream().findFirst()).map(e -> texto(e, "Identifier"))
                .filter(id -> !id.isEmpty()).orElse("default");
    }

    /** WGS84BoundingBox: "longitud latitud" en sus dos esquinas. */
    private static Extension extension(Element capa) {
        return hijosLlamados(capa, "WGS84BoundingBox").stream().findFirst().map(caja -> {
            double[] inferior = numeros(texto(caja, "LowerCorner"));
            double[] superior = numeros(texto(caja, "UpperCorner"));
            return inferior.length == 2 && superior.length == 2
                    ? new Extension(inferior[0], inferior[1], superior[0], superior[1])
                    : null;
        }).orElse(null);
    }

    private static Grilla grilla(Element grilla) {
        String crs = crs(texto(grilla, "SupportedCRS"));
        // EPSG:4326 se escribe latitud primero; CRS:84 y EPSG:3857, x primero.
        boolean latitudPrimero = "EPSG:4326".equals(crs) && !texto(grilla, "SupportedCRS").toUpperCase(Locale.ROOT).contains("CRS84");
        double metrosPorUnidad = "EPSG:4326".equals(crs) ? METROS_POR_GRADO : 1;
        List<MatrizMosaicos> matrices = new ArrayList<>();
        for (Element matriz : hijosLlamados(grilla, "TileMatrix")) {
            double[] esquina = numeros(texto(matriz, "TopLeftCorner"));
            try {
                double escala = Double.parseDouble(texto(matriz, "ScaleDenominator"));
                matrices.add(new MatrizMosaicos(texto(matriz, "Identifier"), escala * PIXEL_METROS / metrosPorUnidad,
                        esquina[latitudPrimero ? 1 : 0], esquina[latitudPrimero ? 0 : 1],
                        Integer.parseInt(texto(matriz, "TileWidth")), Integer.parseInt(texto(matriz, "TileHeight")),
                        Integer.parseInt(texto(matriz, "MatrixWidth")), Integer.parseInt(texto(matriz, "MatrixHeight"))));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // Una matriz mal escrita deja la grilla fuera, en vez de dibujarla mal.
                return new Grilla(texto(grilla, "Identifier"), "", List.of());
            }
        }
        matrices.sort(Comparator.comparingDouble(MatrizMosaicos::resolucion).reversed());
        return new Grilla(texto(grilla, "Identifier"), matrices.isEmpty() ? "" : crs, matrices);
    }

    /** Las formas en que los servidores escriben los dos sistemas que el mapa dibuja. */
    private static String crs(String soportado) {
        String crs = soportado.toUpperCase(Locale.ROOT);
        if (crs.matches(".*\\b(3857|900913|102100|3785)$")) {
            return "EPSG:3857";
        }
        return crs.matches(".*(\\b4326|CRS84)$") ? "EPSG:4326" : "";
    }

    /** OperationsMetadata/Operation[GetTile]/DCP/HTTP/Get/@xlink:href. */
    private static String urlGetTile(Element raiz) {
        return hijosLlamados(raiz, "OperationsMetadata").stream()
                .flatMap(operaciones -> hijosLlamados(operaciones, "Operation").stream())
                .filter(operacion -> "GetTile".equals(operacion.getAttribute("name")))
                .flatMap(operacion -> hijosLlamados(operacion, "DCP").stream())
                .flatMap(dcp -> hijosLlamados(dcp, "HTTP").stream())
                .flatMap(http -> hijosLlamados(http, "Get").stream())
                .map(get -> get.getAttributeNS("http://www.w3.org/1999/xlink", "href").trim())
                .filter(href -> !href.isEmpty())
                .findFirst().orElse("");
    }

    private static double[] numeros(String texto) {
        try {
            String[] partes = texto.trim().split("\\s+");
            double[] numeros = new double[partes.length];
            for (int i = 0; i < partes.length; i++) {
                numeros[i] = Double.parseDouble(partes[i]);
            }
            return numeros;
        } catch (NumberFormatException e) {
            return new double[0];
        }
    }

    private static List<Element> hijosLlamados(Element padre, String nombre) {
        return hijos(padre).stream().filter(h -> nombre.equals(h.getLocalName())).toList();
    }
}
