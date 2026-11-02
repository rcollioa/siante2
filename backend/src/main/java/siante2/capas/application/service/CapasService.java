package siante2.capas.application.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import siante2.capas.application.port.in.CambiarVisibilidadCapaUseCase;
import siante2.capas.application.port.in.ConsultarPuntoUseCase;
import siante2.capas.application.port.in.EditarCapaUseCase;
import siante2.capas.application.port.in.EliminarCapaUseCase;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.ObtenerMapaUseCase;
import siante2.capas.application.port.in.ObtenerMosaicoUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.application.port.out.CapasWritePort;
import siante2.capas.application.port.out.ServiciosMapaPort;
import siante2.capas.application.port.out.ServiciosWmtsPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.ConsultaPunto;
import siante2.capas.domain.EdicionCapa;
import siante2.capas.domain.ElementoDeServicio;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.PedidoMosaico;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.ServicioMapa;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.TipoServicio;
import siante2.capas.domain.Visibilidad;
import siante2.categorias.application.port.in.ListarCategoriasUseCase;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;

/** El dueño sale de la identidad de WebSEAL, nunca de la solicitud (SIATE lo tomaba de la URL). */
@Service
public class CapasService
        implements ListarMisCapasUseCase, VerCapaUseCase, ListarFavoritasUseCase, VerCatalogoUseCase, ObtenerMapaUseCase,
        ConsultarPuntoUseCase, ObtenerMosaicoUseCase, EditarCapaUseCase, CambiarVisibilidadCapaUseCase, EliminarCapaUseCase {

    private static final Logger log = LoggerFactory.getLogger(CapasService.class);
    private static final Locale ESPANOL = Locale.of("es", "CL");
    /** ID que ningún usuario tiene: sin registro en USERS no hay capas propias ni favoritas. */
    private static final int SIN_USUARIO = -1;
    /** Los del formulario "Editar capa" de SIATE, que solo los validaba en el navegador. */
    private static final int MINIMO_NOMBRE = 4;
    private static final int MAXIMO_NOMBRE = 100;
    private static final int MAXIMO_DESCRIPCION = 200;

    private final CapasReadPort capas;
    private final CapasWritePort escritura;
    private final IdentificarUsuarioUseCase usuarios;
    private final ListarCategoriasUseCase categorias;
    private final ServiciosMapaPort servicios;
    private final ServiciosWmtsPort mosaicos;

    public CapasService(CapasReadPort capas, CapasWritePort escritura, IdentificarUsuarioUseCase usuarios,
                        ListarCategoriasUseCase categorias, ServiciosMapaPort servicios, ServiciosWmtsPort mosaicos) {
        this.capas = capas;
        this.escritura = escritura;
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.servicios = servicios;
        this.mosaicos = mosaicos;
    }

    @Override
    public List<CapaPropia> listar(String ivUser) {
        return porNombre(usuarios.idDe(ivUser).map(capas::listarDe).orElse(List.of()), CapaPropia::nombre);
    }

    @Override
    public void editar(int capaId, String ivUser, EdicionCapa edicion) {
        EdicionCapa valida = validada(edicion);
        exigirCapaPropia(escritura.actualizar(capaId, dueno(ivUser), valida));
        log.info("Capa {} editada", capaId);
    }

    @Override
    public void cambiarVisibilidad(int capaId, String ivUser, Visibilidad visibilidad) {
        exigirPrivadaOPublica(visibilidad);
        exigirCapaPropia(escritura.cambiarVisibilidad(capaId, dueno(ivUser), visibilidad));
        log.info("Capa {} ahora es {}", capaId, visibilidad);
    }

    @Override
    public void eliminar(int capaId, String ivUser) {
        exigirCapaPropia(escritura.eliminar(capaId, dueno(ivUser)));
        log.info("Capa {} eliminada", capaId);
    }

    private int dueno(String ivUser) {
        return usuarios.idDe(ivUser).orElseThrow(CapasService::noExiste);
    }

    private static void exigirCapaPropia(boolean actualizada) {
        if (!actualizada) {
            throw noExiste();
        }
    }

    /** Sin espacios a los lados: SIATE aceptaba un nombre de solo espacios. */
    private EdicionCapa validada(EdicionCapa edicion) {
        String nombre = edicion.nombre() == null ? "" : edicion.nombre().trim();
        String descripcion = edicion.descripcion() == null ? "" : edicion.descripcion().trim();
        if (nombre.isEmpty()) {
            throw new SolicitudInvalidaException("Nombre requerido.");
        }
        if (nombre.length() < MINIMO_NOMBRE) {
            throw new SolicitudInvalidaException("El nombre debe tener al menos " + MINIMO_NOMBRE + " caracteres.");
        }
        if (nombre.length() > MAXIMO_NOMBRE) {
            throw new SolicitudInvalidaException("El nombre no puede tener más de " + MAXIMO_NOMBRE + " caracteres.");
        }
        if (descripcion.length() > MAXIMO_DESCRIPCION) {
            throw new SolicitudInvalidaException("La descripción no puede tener más de " + MAXIMO_DESCRIPCION + " caracteres.");
        }
        exigirPrivadaOPublica(edicion.visibilidad());
        if (categorias.listarActivas().stream().noneMatch(categoria -> categoria.id() == edicion.categoriaId())) {
            throw new SolicitudInvalidaException("La categoría no existe.");
        }
        return new EdicionCapa(nombre, descripcion, edicion.categoriaId(), edicion.visibilidad());
    }

    /** Formal y área de dibujo no se eligen: SIATE solo ofrece público o privado, y aceptaba cualquier código. */
    private static void exigirPrivadaOPublica(Visibilidad visibilidad) {
        if (visibilidad != Visibilidad.PRIVADA && visibilidad != Visibilidad.PUBLICA) {
            throw new SolicitudInvalidaException("Una capa solo puede ser privada o pública.");
        }
    }

    @Override
    public List<ResumenCapa> listarFavoritas(String ivUser) {
        return porNombre(usuarios.idDe(ivUser).map(capas::listarFavoritasDe).orElse(List.of()), ResumenCapa::nombre);
    }

    /** Un usuario que nunca ingresó ve igual las capas públicas y formales, sin ninguna favorita ni propia. */
    @Override
    public List<CapaCatalogo> verCatalogo(SeccionCatalogo seccion, String ivUser) {
        int usuario = usuarios.idDe(ivUser).orElse(SIN_USUARIO);
        return porNombre(capas.catalogo(seccion, usuario), CapaCatalogo::nombre);
    }

    /** Por nombre como en un diccionario: "Árboles" junto a "Arroyos" y no después de la Z, como en la base. */
    private static <T> List<T> porNombre(List<T> lista, Function<T, String> nombre) {
        return lista.stream().sorted(Comparator.comparing(nombre, Collator.getInstance(ESPANOL))).toList();
    }

    @Override
    public Capa ver(int capaId, String ivUser) {
        int usuario = usuarios.idDe(ivUser).orElseThrow(CapasService::noExiste);
        Capa capa = capas.buscarVisible(capaId, usuario).orElseThrow(CapasService::noExiste);
        if (capa.tipo() != TipoCapa.URL) {
            return capa;
        }
        // Como SIATE al abrirla: las capas del servidor salen de su GetCapabilities. WFS, sin capas activas, no se dibuja.
        return capas.servicioVisible(capaId, usuario).map(origen -> capa.conServicio(servicio(origen))).orElse(capa);
    }

    /** Si el servidor no responde, la capa se abre igual sin contenido, como en SIATE ({@code ERROR} en
     * {@code getFullLayerById}): el usuario la ve en sus capas abiertas con el aviso, en vez de un error. */
    private ServicioMapa servicio(OrigenServicio origen) {
        try {
            return switch (origen.tipo()) {
                case WMS -> new ServicioMapa(origen.tipo(), servicios.capasWms(origen.url()));
                case WMTS -> mosaicos.servicioWmts(origen.url());
                case WFS -> new ServicioMapa(origen.tipo(), List.of());
            };
        } catch (ServicioExternoNoDisponibleException e) {
            return ServicioMapa.noDisponible(origen.tipo());
        }
    }

    /** Como las imágenes WMS: solo capas que el servidor publica y mosaicos dentro de su grilla. */
    @Override
    public ImagenMapa obtenerMosaico(int capaId, String ivUser, PedidoMosaico pedido) {
        OrigenServicio origen = origenVisible(capaId, ivUser, TipoServicio.WMTS);
        ServicioMapa servicio = mosaicos.servicioWmts(origen.url());
        boolean publicada = servicio.capas().stream().anyMatch(capa -> capa.nombre().equals(pedido.capa()));
        boolean enLaGrilla = servicio.grilla().matriz(pedido.matriz()).filter(pedido::dentroDe).isPresent();
        if (!publicada || !enLaGrilla) {
            throw new SolicitudInvalidaException("El servidor de esta capa no publica el mosaico pedido.");
        }
        return mosaicos.mosaicoWmts(origen.url(), pedido);
    }

    private OrigenServicio origenVisible(int capaId, String ivUser, TipoServicio tipo) {
        return usuarios.idDe(ivUser)
                .flatMap(usuario -> capas.servicioVisible(capaId, usuario))
                .filter(servicio -> servicio.tipo() == tipo)
                .orElseThrow(CapasService::noExiste);
    }

    /** Solo capas que el servidor de esta capa publica: el backend no es un proxy abierto a cualquier capa ni URL. */
    @Override
    public ImagenMapa obtenerMapa(int capaId, String ivUser, PedidoMapa pedido) {
        return servicios.mapaWms(servidorWms(capaId, ivUser, pedido).url(), pedido);
    }

    /** Con el título de cada capa del servidor, el mismo que se ve en la lista de la capa. */
    @Override
    public List<ElementoDeServicio> consultarPunto(int capaId, String ivUser, ConsultaPunto consulta) {
        ServidorWms servidor = servidorWms(capaId, ivUser, consulta.mapa());
        Map<String, String> titulos = servidor.publicadas().stream()
                .collect(Collectors.toMap(CapaDeServicio::nombre, CapaDeServicio::titulo, (primero, otro) -> primero));
        return servicios.infoWms(servidor.url(), consulta).stream()
                .map(elemento -> elemento.conCapa(titulos.getOrDefault(elemento.capa(), elemento.capa())))
                .toList();
    }

    /** El servidor de una capa WMS que el usuario puede ver, si publica las capas pedidas. */
    private ServidorWms servidorWms(int capaId, String ivUser, PedidoMapa pedido) {
        OrigenServicio origen = origenVisible(capaId, ivUser, TipoServicio.WMS);
        List<CapaDeServicio> publicadas = servicios.capasWms(origen.url());
        if (!publicadas.stream().map(CapaDeServicio::nombre).toList().containsAll(pedido.capas())) {
            throw new SolicitudInvalidaException("El servidor de esta capa no publica las capas pedidas.");
        }
        return new ServidorWms(origen.url(), publicadas);
    }

    private record ServidorWms(String url, List<CapaDeServicio> publicadas) {
    }

    private static RecursoNoEncontradoException noExiste() {
        return new RecursoNoEncontradoException("La capa no existe.");
    }
}
