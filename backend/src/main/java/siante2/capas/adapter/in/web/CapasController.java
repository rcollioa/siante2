package siante2.capas.adapter.in.web;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.ObtenerMapaUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;

/** Reemplaza a {@code GET /siante/api/users/:ID/layers} y {@code GET /siante/api/layers/:ID} de SIATE. El usuario
 * sale de WebSEAL y no de la URL; sin {@code @Catalogo}, porque son datos de cada usuario. */
@RestController
@RequestMapping("/capas")
public class CapasController {

    private final ListarMisCapasUseCase listar;
    private final VerCapaUseCase ver;
    private final ListarFavoritasUseCase favoritas;
    private final VerCatalogoUseCase catalogo;
    private final ObtenerMapaUseCase mapa;

    public CapasController(ListarMisCapasUseCase listar, VerCapaUseCase ver, ListarFavoritasUseCase favoritas,
                           VerCatalogoUseCase catalogo, ObtenerMapaUseCase mapa) {
        this.listar = listar;
        this.ver = ver;
        this.favoritas = favoritas;
        this.catalogo = catalogo;
        this.mapa = mapa;
    }

    @GetMapping("/mias")
    public CapasResponse listarMisCapas(Authentication autenticacion) {
        return new CapasResponse(listar.listar(autenticacion.getName()));
    }

    /** Reemplaza a {@code users/:ID/favourites} y {@code users/:ID/favourites/formallayers} de SIATE: las dos listas
     * del menú en una sola respuesta, que el frontend separa por visibilidad. */
    @GetMapping("/favoritas")
    public CapasResponse listarCapasFavoritas(Authentication autenticacion) {
        return new CapasResponse(favoritas.listarFavoritas(autenticacion.getName()));
    }

    /** Las pestañas del catálogo de SIATE ("Todas", "Usuario" y "Capas formales"). */
    @GetMapping("/catalogo")
    public CatalogoResponse verCatalogoDeCapas(@RequestParam SeccionCatalogo seccion, Authentication autenticacion) {
        return new CatalogoResponse(catalogo.verCatalogo(seccion, autenticacion.getName()));
    }

    /** Imagen WMS (GetMap) de una capa por URL, pedida a su servidor por el backend: el navegador no habla con
     * terceros (CSP) y solo se consulta el servidor que registró la capa. Reemplaza al {@code TileWMS} de SIATE, que
     * iba directo del navegador al servidor. Los parámetros llevan el nombre que usa WMS (OpenLayers los manda así). */
    @GetMapping("/{id}/wms")
    public ResponseEntity<byte[]> obtenerMapaDeCapa(@PathVariable int id, @RequestParam("LAYERS") String capas,
            @RequestParam("BBOX") String bbox, @RequestParam("WIDTH") int ancho, @RequestParam("HEIGHT") int alto,
            @RequestParam(value = "CRS", required = false) String crs,
            @RequestParam(value = "SRS", required = false) String srs,
            @RequestParam(value = "FORMAT", defaultValue = "image/png") String formato,
            @RequestParam(value = "TRANSPARENT", defaultValue = "true") boolean transparente,
            @RequestParam(value = "VERSION", defaultValue = "1.3.0") String version,
            Authentication autenticacion) {
        PedidoMapa pedido = new PedidoMapa(Arrays.asList(capas.split(",")), bbox, ancho, alto, crs != null ? crs : srs,
                formato, transparente, version);
        ImagenMapa imagen = mapa.obtenerMapa(id, autenticacion.getName(), pedido);
        // Privado como toda la API; unos minutos bastan para moverse por el mapa sin volver a pedir cada mosaico.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .contentType(MediaType.parseMediaType(imagen.tipo()))
                .body(imagen.contenido());
    }

    @GetMapping("/{id}")
    public Capa verCapa(@PathVariable int id, Authentication autenticacion) {
        return ver.ver(id, autenticacion.getName());
    }

    public record CapasResponse(List<ResumenCapa> data) {
    }

    public record CatalogoResponse(List<CapaCatalogo> data) {
    }
}
