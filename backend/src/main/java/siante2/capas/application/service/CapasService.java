package siante2.capas.application.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import siante2.capas.application.port.in.ListarFavoritasUseCase;
import siante2.capas.application.port.in.ListarMisCapasUseCase;
import siante2.capas.application.port.in.ObtenerMapaUseCase;
import siante2.capas.application.port.in.VerCapaUseCase;
import siante2.capas.application.port.in.VerCatalogoUseCase;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.application.port.out.ServiciosMapaPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaDeServicio;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.PedidoMapa;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.ServicioMapa;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.TipoServicio;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;

/** El dueño sale de la identidad de WebSEAL, nunca de la solicitud (SIATE lo tomaba de la URL). */
@Service
public class CapasService
        implements ListarMisCapasUseCase, VerCapaUseCase, ListarFavoritasUseCase, VerCatalogoUseCase, ObtenerMapaUseCase {

    private static final Locale ESPANOL = Locale.of("es", "CL");
    /** ID que ningún usuario tiene: sin registro en USERS no hay capas propias ni favoritas. */
    private static final int SIN_USUARIO = -1;

    private final CapasReadPort capas;
    private final IdentificarUsuarioUseCase usuarios;
    private final ServiciosMapaPort servicios;

    public CapasService(CapasReadPort capas, IdentificarUsuarioUseCase usuarios, ServiciosMapaPort servicios) {
        this.capas = capas;
        this.usuarios = usuarios;
        this.servicios = servicios;
    }

    @Override
    public List<ResumenCapa> listar(String ivUser) {
        return porNombre(usuarios.idDe(ivUser).map(capas::listarDe).orElse(List.of()), ResumenCapa::nombre);
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
        // Como SIATE al abrirla: las capas del servidor salen de su GetCapabilities. WMTS y WFS, en un próximo paso.
        return capas.servicioVisible(capaId, usuario)
                .map(origen -> capa.conServicio(new ServicioMapa(origen.tipo(),
                        origen.tipo() == TipoServicio.WMS ? servicios.capasWms(origen.url()) : List.of())))
                .orElse(capa);
    }

    /** Solo capas que el servidor de esta capa publica: el backend no es un proxy abierto a cualquier capa ni URL. */
    @Override
    public ImagenMapa obtenerMapa(int capaId, String ivUser, PedidoMapa pedido) {
        OrigenServicio origen = usuarios.idDe(ivUser)
                .flatMap(usuario -> capas.servicioVisible(capaId, usuario))
                .filter(servicio -> servicio.tipo() == TipoServicio.WMS)
                .orElseThrow(CapasService::noExiste);
        List<String> publicadas = servicios.capasWms(origen.url()).stream().map(CapaDeServicio::nombre).toList();
        if (!publicadas.containsAll(pedido.capas())) {
            throw new SolicitudInvalidaException("El servidor de esta capa no publica las capas pedidas.");
        }
        return servicios.mapaWms(origen.url(), pedido);
    }

    private static RecursoNoEncontradoException noExiste() {
        return new RecursoNoEncontradoException("La capa no existe.");
    }
}
