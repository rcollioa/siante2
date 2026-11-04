package siante2.capas.domain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tipo de servidor de mapas de una capa por URL, como lo guarda SIATE en {@code JSON_METADATA.type}. */
public enum TipoServicio {
    WMS, WMTS, WFS;

    private static final Pattern PARAMETRO = Pattern.compile("[?&]service=(wms|wmts|wfs)(&|$)", Pattern.CASE_INSENSITIVE);

    /** Como SIATE, por el texto de la URL y WMS si no dice otra cosa. A diferencia de SIATE manda el parámetro
     * {@code SERVICE} y WMTS gana a WMS, para que {@code wms.ide.cl/wmts} no se tome por un WMS. */
    public static TipoServicio deUrl(String url) {
        Matcher parametro = PARAMETRO.matcher(url);
        if (parametro.find()) {
            return valueOf(parametro.group(1).toUpperCase(Locale.ROOT));
        }
        String minuscula = url.toLowerCase(Locale.ROOT);
        if (minuscula.contains("wmts")) {
            return WMTS;
        }
        return minuscula.contains("wfs") ? WFS : WMS;
    }
}
