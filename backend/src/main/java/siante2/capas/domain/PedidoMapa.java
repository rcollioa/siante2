package siante2.capas.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import siante2.compartido.domain.SolicitudInvalidaException;

/** Una imagen de mapa WMS (GetMap) que pide el navegador. Solo se aceptan valores que un mapa necesita: el backend
 * no reenvía parámetros arbitrarios a otro servidor. */
public record PedidoMapa(List<String> capas, String bbox, int ancho, int alto, String crs, String formato,
                         boolean transparente, String version) {

    /** Como un mosaico de OpenLayers en pantallas de alta densidad, con margen. */
    public static final int LADO_MAXIMO = 2048;
    private static final Set<String> CRS = Set.of("EPSG:3857", "EPSG:900913", "EPSG:4326");
    private static final Set<String> FORMATOS = Set.of("image/png", "image/jpeg");
    private static final Set<String> VERSIONES = Set.of("1.1.1", "1.3.0");
    private static final Pattern NUMERO = Pattern.compile("-?\\d+(\\.\\d+)?([eE][-+]?\\d+)?");

    public PedidoMapa {
        if (capas == null || capas.isEmpty() || capas.stream().anyMatch(c -> c == null || c.isBlank())) {
            throw new SolicitudInvalidaException("Indique las capas del mapa.");
        }
        String[] esquinas = bbox == null ? new String[0] : bbox.split(",");
        if (esquinas.length != 4 || !Arrays.stream(esquinas).allMatch(n -> NUMERO.matcher(n.trim()).matches())) {
            throw new SolicitudInvalidaException("La extensión del mapa no es válida.");
        }
        if (ancho < 1 || alto < 1 || ancho > LADO_MAXIMO || alto > LADO_MAXIMO) {
            throw new SolicitudInvalidaException("El tamaño de la imagen no es válido.");
        }
        if (!CRS.contains(crs) || !FORMATOS.contains(formato) || !VERSIONES.contains(version)) {
            throw new SolicitudInvalidaException("El sistema de coordenadas, el formato o la versión no son válidos.");
        }
        capas = List.copyOf(capas);
    }
}
