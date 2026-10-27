package siante2.direcciones.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.direcciones.application.port.in.BuscarDireccionesUseCase;
import siante2.direcciones.application.port.in.SugerirDireccionesUseCase;
import siante2.direcciones.application.port.out.AutocompletadoPort;
import siante2.direcciones.application.port.out.GeocodificadorPort;
import siante2.direcciones.domain.Direccion;
import siante2.direcciones.domain.ZonaVisible;

@Service
public class DireccionesService implements BuscarDireccionesUseCase, SugerirDireccionesUseCase {

    static final int LARGO_MINIMO = 3;
    static final int LARGO_MAXIMO = 200;
    /** El máximo de OpenLayers. */
    static final int ZOOM_MAXIMO = 28;

    private final GeocodificadorPort geocodificador;
    private final AutocompletadoPort autocompletado;

    public DireccionesService(GeocodificadorPort geocodificador, AutocompletadoPort autocompletado) {
        this.geocodificador = geocodificador;
        this.autocompletado = autocompletado;
    }

    @Override
    public List<Direccion> buscar(String texto) {
        return geocodificador.buscar(normalizar(texto));
    }

    @Override
    public List<Direccion> sugerir(String texto, Optional<ZonaVisible> zona) {
        return autocompletado.sugerir(normalizar(texto), zona.map(DireccionesService::redondear));
    }

    /** A unos 1 km: mover un poco el mapa no cambia la consulta (la caché la reconoce) y al servicio externo le
     * llega una referencia aproximada. La comparación negada también rechaza NaN. */
    private static ZonaVisible redondear(ZonaVisible zona) {
        if (!(Math.abs(zona.latitud()) <= 90 && Math.abs(zona.longitud()) <= 180)
                || zona.zoom() < 0 || zona.zoom() > ZOOM_MAXIMO) {
            throw new SolicitudInvalidaException("La zona del mapa no es válida.");
        }
        return new ZonaVisible(Math.round(zona.latitud() * 100) / 100.0, Math.round(zona.longitud() * 100) / 100.0,
                zona.zoom());
    }

    /** Normaliza espacios y mayúsculas para que la caché reconozca la misma búsqueda. */
    private static String normalizar(String texto) {
        String normalizado = texto == null ? "" : texto.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (normalizado.length() < LARGO_MINIMO || normalizado.length() > LARGO_MAXIMO) {
            throw new SolicitudInvalidaException(
                    "La búsqueda debe tener entre " + LARGO_MINIMO + " y " + LARGO_MAXIMO + " caracteres.");
        }
        return normalizado;
    }
}
