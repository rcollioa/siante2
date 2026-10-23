package siante2.direcciones.adapter.in.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import siante2.direcciones.application.port.in.BuscarDireccionesUseCase;
import siante2.direcciones.domain.Direccion;

/** Reemplaza la llamada directa del navegador a Nominatim que hacía SIANTE (control Geocoder del mapa). */
@RestController
@RequestMapping("/direcciones")
public class DireccionesController {

    private final BuscarDireccionesUseCase buscar;

    public DireccionesController(BuscarDireccionesUseCase buscar) {
        this.buscar = buscar;
    }

    @GetMapping
    public DireccionesResponse buscarDirecciones(@RequestParam String texto) {
        return new DireccionesResponse(buscar.buscar(texto));
    }

    public record DireccionesResponse(List<Direccion> data) {
    }
}
