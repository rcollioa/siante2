package siante2.capas.adapter.in.web;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import siante2.capas.application.port.in.CambiarVisibilidadCapaUseCase;
import siante2.capas.application.port.in.ConsultarPuntoUseCase;
import siante2.capas.application.port.in.CrearCapaUseCase;
import siante2.capas.application.port.in.EditarCapaUseCase;
import siante2.capas.application.port.in.EliminarCapaUseCase;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.ObtenerMapaUseCase;
import siante2.capas.application.port.in.ObtenerMosaicoUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.EdicionCapa;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.NuevaCapa;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.PedidoMosaico;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.Visibilidad;

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
    private final ConsultarPuntoUseCase consulta;
    private final ObtenerMosaicoUseCase mosaico;
    private final CrearCapaUseCase crear;
    private final EditarCapaUseCase editar;
    private final CambiarVisibilidadCapaUseCase visibilidad;
    private final EliminarCapaUseCase eliminar;

    public CapasController(ListarMisCapasUseCase listar, VerCapaUseCase ver, ListarFavoritasUseCase favoritas,
                           VerCatalogoUseCase catalogo, ObtenerMapaUseCase mapa, ConsultarPuntoUseCase consulta,
                           ObtenerMosaicoUseCase mosaico, CrearCapaUseCase crear, EditarCapaUseCase editar,
                           CambiarVisibilidadCapaUseCase visibilidad, EliminarCapaUseCase eliminar) {
        this.crear = crear;
        this.editar = editar;
        this.visibilidad = visibilidad;
        this.eliminar = eliminar;
        this.listar = listar;
        this.ver = ver;
        this.favoritas = favoritas;
        this.catalogo = catalogo;
        this.mapa = mapa;
        this.consulta = consulta;
        this.mosaico = mosaico;
    }

    @GetMapping("/mias")
    public MisCapasResponse listarMisCapas(Authentication autenticacion) {
        return new MisCapasResponse(listar.listar(autenticacion.getName()));
    }

    /** Una capa en blanco. Reemplaza a {@code POST /siante/api/layers} de SIATE, que tomaba el dueño del cuerpo
     * ({@code OWNER}) y buscaba después el ID más alto del usuario para saber cuál había creado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CapaPropia crearCapa(@RequestBody NuevaCapa nueva, Authentication autenticacion) {
        return crear.crear(autenticacion.getName(), nueva);
    }

    /** Reemplaza a {@code PATCH /siante/api/layers/:ID} de SIATE, que no verificaba el dueño y dejaba cambiar cualquier
     * campo, incluido {@code OWNER}. Aquí, solo los del formulario y solo en capas propias. */
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarCapa(@PathVariable int id, @RequestBody EdicionCapa edicion, Authentication autenticacion) {
        editar.editar(id, autenticacion.getName(), edicion);
    }

    /** Publicar y despublicar, los botones de "Mis capas" en SIATE. */
    @PatchMapping("/{id}/visibilidad")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarVisibilidadDeCapa(@PathVariable int id, @RequestBody CambioVisibilidad cambio, Authentication autenticacion) {
        visibilidad.cambiarVisibilidad(id, autenticacion.getName(), cambio.visibilidad());
    }

    /** POST y no DELETE: SIATE solo usa POST y PATCH, los únicos verbos que se sabe que WebSEAL deja pasar. Reemplaza al
     * {@code PATCH} con {@code STATUS=0}: como en SIATE, la capa queda inactiva y no se borra. */
    @PostMapping("/{id}/eliminar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCapa(@PathVariable int id, Authentication autenticacion) {
        eliminar.eliminar(id, autenticacion.getName());
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

    /** Mosaico WMTS (GetTile) de una capa por URL, pedido por el backend a su servidor, como las imágenes WMS.
     * Reemplaza al {@code ol.source.WMTS} de SIATE, que iba directo del navegador al servidor. La ruta sigue la
     * plantilla REST de WMTS, que OpenLayers completa con la matriz, la fila y la columna. */
    @GetMapping("/{id}/wmts/{capa}/{matriz}/{fila}/{columna}")
    public ResponseEntity<byte[]> obtenerMosaicoDeCapa(@PathVariable int id, @PathVariable String capa,
            @PathVariable String matriz, @PathVariable int fila, @PathVariable int columna, Authentication autenticacion) {
        ImagenMapa imagen = mosaico.obtenerMosaico(id, autenticacion.getName(), new PedidoMosaico(capa, matriz, fila, columna));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .contentType(MediaType.parseMediaType(imagen.tipo()))
                .body(imagen.contenido());
    }

    /** Qué hay en un punto de una capa por URL (GetFeatureInfo): la imagen en que se hizo clic y el píxel dentro de
     * ella. Reemplaza a {@code GET /siante/api/layers/featureinfo?URL=} de SIATE, que pedía cualquier URL que le
     * mandara el navegador; aquí, como las imágenes, solo al servidor de la capa y con capas que publica. */
    @GetMapping("/{id}/wms/info")
    public ElementosDeServicioResponse consultarPuntoDeCapa(@PathVariable int id, @RequestParam("LAYERS") String capas,
            @RequestParam("BBOX") String bbox, @RequestParam("WIDTH") int ancho, @RequestParam("HEIGHT") int alto,
            @RequestParam("CRS") String crs, @RequestParam(value = "VERSION", defaultValue = "1.3.0") String version,
            @RequestParam("I") int i, @RequestParam("J") int j, Authentication autenticacion) {
        PedidoMapa imagen = new PedidoMapa(Arrays.asList(capas.split(",")), bbox, ancho, alto, crs, "image/png", true,
                version);
        return new ElementosDeServicioResponse(
                consulta.consultarPunto(id, autenticacion.getName(), new ConsultaPunto(imagen, i, j)));
    }

    @GetMapping("/{id}")
    public Capa verCapa(@PathVariable int id, Authentication autenticacion) {
        return ver.ver(id, autenticacion.getName());
    }

    public record MisCapasResponse(List<CapaPropia> data) {
    }

    public record CapasResponse(List<ResumenCapa> data) {
    }

    public record CambioVisibilidad(Visibilidad visibilidad) {
    }

    public record ElementosDeServicioResponse(List<ElementoDeServicio> data) {
    }

    public record CatalogoResponse(List<CapaCatalogo> data) {
    }
}
