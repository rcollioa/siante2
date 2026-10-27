package siante2.direcciones.adapter.in.web;

import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import siante2.direcciones.application.port.in.BuscarDireccionesUseCase;
import siante2.direcciones.application.port.in.SugerirDireccionesUseCase;
import siante2.direcciones.domain.Direccion;
import siante2.direcciones.domain.ZonaVisible;

/** Reemplaza la llamada directa del navegador a Nominatim que hacía SIATE (control Geocoder del mapa). */
@RestController
@RequestMapping("/direcciones")
public class DireccionesController {

    private final BuscarDireccionesUseCase buscar;
    private final SugerirDireccionesUseCase sugerir;

    public DireccionesController(BuscarDireccionesUseCase buscar, SugerirDireccionesUseCase sugerir) {
        this.buscar = buscar;
        this.sugerir = sugerir;
    }

    @GetMapping
    public DireccionesResponse buscarDirecciones(@RequestParam String texto) {
        return new DireccionesResponse(buscar.buscar(texto));
    }

    /** Mientras se escribe, como el autocompletado de SIATE, pero sin pedirle a Nominatim (lo prohíbe). Con la
     * zona del mapa (latitud, longitud y zoom) las cercanas aparecen primero; sin las tres, todo Chile por igual. */
    @GetMapping("/sugerencias")
    public DireccionesResponse sugerirDirecciones(@RequestParam String texto,
            @RequestParam(required = false) Double latitud, @RequestParam(required = false) Double longitud,
            @RequestParam(required = false) Integer zoom) {
        Optional<ZonaVisible> zona = latitud == null || longitud == null || zoom == null
                ? Optional.empty()
                : Optional.of(new ZonaVisible(latitud, longitud, zoom));
        return new DireccionesResponse(sugerir.sugerir(texto, zona));
    }

    public record DireccionesResponse(List<Direccion> data) {
    }
}
