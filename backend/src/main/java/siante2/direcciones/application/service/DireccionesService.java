package siante2.direcciones.application.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.direcciones.application.port.in.BuscarDireccionesUseCase;
import siante2.direcciones.application.port.out.GeocodificadorPort;
import siante2.direcciones.domain.Direccion;

@Service
public class DireccionesService implements BuscarDireccionesUseCase {

    static final int LARGO_MINIMO = 3;
    static final int LARGO_MAXIMO = 200;

    private final GeocodificadorPort geocodificador;

    public DireccionesService(GeocodificadorPort geocodificador) {
        this.geocodificador = geocodificador;
    }

    /** Normaliza espacios y mayúsculas para que la caché reconozca la misma búsqueda. */
    @Override
    public List<Direccion> buscar(String texto) {
        String normalizado = texto == null ? "" : texto.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (normalizado.length() < LARGO_MINIMO || normalizado.length() > LARGO_MAXIMO) {
            throw new SolicitudInvalidaException(
                    "La búsqueda debe tener entre " + LARGO_MINIMO + " y " + LARGO_MAXIMO + " caracteres.");
        }
        return geocodificador.buscar(normalizado);
    }
}
