package siante2.capas.adapter.out.oracle;

import java.util.regex.Pattern;

/** Lo que los primeros caracteres de {@code LAYERS.JSON_METADATA} dicen de una capa, sin leer el CLOB completo. Solo
 * cuenta lo que hay antes de sus elementos, donde un dato importado podría llamarse igual. */
final class InicioJsonCapa {

    /** Antes de los elementos, SIATE guarda el tipo, el zoom, el centro, los datos de contacto y la URL. */
    static final int LARGO = 400;

    /** SIATE guarda una capa por URL como {@code {"type":"WMS","url":...}}. */
    private static final Pattern POR_URL = Pattern.compile("\"type\"\\s*:\\s*\"(WMS|WFS|WMTS)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern CON_URL = Pattern.compile("\"url\"\\s*:");
    private static final String ELEMENTOS = "\"features\"";

    private InicioJsonCapa() {
    }

    static boolean esPorUrl(String inicio) {
        return inicio != null && POR_URL.matcher(encabezado(inicio)).find();
    }

    /** Ni las capas por URL ni las colecciones traídas de una URL, que SIATE vuelve a descargar al sincronizarlas: su
     * mapa las abre como base, sin edición ({@code meta.url}). */
    static boolean sePuedeDibujar(String inicio) {
        return inicio == null || !esPorUrl(inicio) && !CON_URL.matcher(encabezado(inicio)).find();
    }

    private static String encabezado(String inicio) {
        int elementos = inicio.indexOf(ELEMENTOS);
        return elementos < 0 ? inicio : inicio.substring(0, elementos);
    }
}
