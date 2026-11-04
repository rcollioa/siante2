package siante2.capas.adapter.out.oracle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.application.port.out.CapasWritePort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.EdicionCapa;
import siante2.capas.domain.NuevaCapa;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.Visibilidad;

@Repository
@Transactional(readOnly = true)
class OracleCapasRepository implements CapasReadPort, CapasWritePort {

    private static final Logger log = LoggerFactory.getLogger(OracleCapasRepository.class);

    /** Códigos de SIATE ({@code MessageConstants}): {@code STATUS} y {@code VISIBILITY}. */
    static final String ACTIVA = "1";
    static final String INACTIVA = "0";
    static final String PRIVADA = "0";
    static final String PUBLICA = "1";
    static final String FORMAL = "2";
    static final String AREA_DE_DIBUJO = "3";

    /** Visibilidades que ve cualquier usuario, además de las propias. */
    private static final List<String> DE_TODOS = List.of(PUBLICA, FORMAL);

    /** Visibilidades de las capas que su dueño gestiona: ni las formales ni las áreas de dibujo. */
    static final List<String> GESTIONABLES = List.of(PRIVADA, PUBLICA);

    private final VistaCapaJpaRepository vista;
    private final ElementoCapaJpaRepository elementos;
    private final CapaJpaRepository capas;
    private final int usuarioCapasFormales;

    /** @param usuarioCapasFormales ID en USERS del usuario de sistema "SIANTE", dueño de las capas formales
     *     ({@code Config.IDSIANTE} del frontend de SIATE). */
    OracleCapasRepository(VistaCapaJpaRepository vista, ElementoCapaJpaRepository elementos, CapaJpaRepository capas,
                          @Value("${app.capas.usuario-formales:1}") int usuarioCapasFormales) {
        this.vista = vista;
        this.elementos = elementos;
        this.capas = capas;
        this.usuarioCapasFormales = usuarioCapasFormales;
    }

    @Override
    public List<CapaPropia> listarDe(int usuarioId) {
        return vista.buscarDe(usuarioId, ACTIVA, AREA_DE_DIBUJO).stream()
                .map(f -> new CapaPropia(f.id(), f.nombre(), f.descripcion(), visibilidad(f.visibilidad()), f.categoriaId()))
                .toList();
    }

    @Override
    @Transactional
    public int crear(int duenoId, NuevaCapa nueva) {
        return capas.save(new CapaEntity(duenoId, nueva.nombre(), nueva.descripcion(), ACTIVA, PRIVADA)).getId();
    }

    @Override
    @Transactional
    public int crearPorUrl(int duenoId, NuevaCapa nueva, OrigenServicio servicio) {
        return capas.save(CapaEntity.porUrl(duenoId, nueva.nombre(), nueva.descripcion(), ACTIVA, PRIVADA,
                ServicioSiate.escribir(servicio))).getId();
    }

    @Override
    @Transactional
    public boolean actualizar(int capaId, int duenoId, EdicionCapa edicion) {
        return capas.actualizar(capaId, duenoId, edicion.nombre(), edicion.descripcion(), edicion.categoriaId(),
                codigo(edicion.visibilidad()), ACTIVA, GESTIONABLES) > 0;
    }

    @Override
    @Transactional
    public boolean cambiarVisibilidad(int capaId, int duenoId, Visibilidad visibilidad) {
        return capas.cambiarVisibilidad(capaId, duenoId, codigo(visibilidad), ACTIVA, GESTIONABLES) > 0;
    }

    @Override
    @Transactional
    public boolean eliminar(int capaId, int duenoId) {
        return capas.desactivar(capaId, duenoId, INACTIVA, ACTIVA, GESTIONABLES) > 0;
    }

    private static String codigo(Visibilidad visibilidad) {
        return switch (visibilidad) {
            case PRIVADA -> PRIVADA;
            case PUBLICA -> PUBLICA;
            case FORMAL -> FORMAL;
            case AREA_DE_DIBUJO -> AREA_DE_DIBUJO;
        };
    }

    @Override
    public Optional<Capa> buscarVisible(int capaId, int usuarioId) {
        return vista.buscarVisible(capaId, usuarioId, ACTIVA, DE_TODOS).map(f -> {
            TipoCapa tipo = InicioJsonCapa.esPorUrl(f.inicioJson()) ? TipoCapa.URL : TipoCapa.VECTORIAL;
            Visibilidad visibilidad = visibilidad(f.visibilidad());
            boolean propia = Objects.equals(f.dueno(), usuarioId);
            boolean editable = propia && (visibilidad == Visibilidad.PRIVADA || visibilidad == Visibilidad.PUBLICA)
                    && InicioJsonCapa.sePuedeDibujar(f.inicioJson());
            return new Capa(f.id(), f.nombre(), f.descripcion(), visibilidad, propia, editable, tipo,
                    tipo == TipoCapa.URL ? List.of() : elementosDe(capaId, usuarioId, f.inicioJson()), null);
        });
    }

    /** Una capa sin filas en {@code LAYERS_FEATURES} puede tener sus elementos en su propio JSON: {@code LayerService.migration}
     * de SIATE los reparte recién después. El CLOB se lee solo en ese caso, y no si la capa está en blanco. */
    private List<ElementoCapa> elementosDe(int capaId, int usuarioId, String inicioJson) {
        List<FilaElementoCapa> filas = elementos.buscarPorCapa(capaId);
        if (!filas.isEmpty()) {
            return sinLosOmitidos(capaId, filas.stream().map(fila -> ElementoSiate.leer(fila.id(), fila.json())).toList());
        }
        if (inicioJson == null || inicioJson.isBlank()) {
            return List.of();
        }
        return vista.buscarJsonVisible(capaId, usuarioId, ACTIVA, DE_TODOS)
                .map(json -> sinLosOmitidos(capaId, ElementoSiate.leerColeccion(json)))
                .orElse(List.of());
    }

    /** Un elemento que no se puede leer se omite: el resto de la capa se muestra igual. */
    private static List<ElementoCapa> sinLosOmitidos(int capaId, List<Optional<ElementoCapa>> leidos) {
        List<ElementoCapa> validos = leidos.stream().flatMap(Optional::stream).toList();
        if (validos.size() < leidos.size()) {
            log.warn("Capa {}: se omitieron {} de {} elementos que no tienen una geometría válida",
                    capaId, leidos.size() - validos.size(), leidos.size());
        }
        return validos;
    }

    @Override
    public Optional<OrigenServicio> servicioVisible(int capaId, int usuarioId) {
        return vista.buscarJsonVisible(capaId, usuarioId, ACTIVA, DE_TODOS).flatMap(ServicioSiate::leer);
    }

    @Override
    public List<ResumenCapa> listarFavoritasDe(int usuarioId) {
        return vista.buscarFavoritasDe(usuarioId, ACTIVA, AREA_DE_DIBUJO, DE_TODOS).stream()
                .map(f -> new ResumenCapa(f.id(), f.nombre(), f.descripcion(), visibilidad(f.visibilidad())))
                .toList();
    }

    @Override
    public List<CapaCatalogo> catalogo(SeccionCatalogo seccion, int usuarioId) {
        List<FilaCapaCatalogo> filas = switch (seccion) {
            case PUBLICAS -> vista.catalogoPublico(usuarioId, ACTIVA, PUBLICA);
            case MIAS -> vista.catalogoDe(usuarioId, usuarioId, ACTIVA, AREA_DE_DIBUJO, DE_TODOS);
            case FORMALES -> vista.catalogoDe(usuarioCapasFormales, usuarioId, ACTIVA, AREA_DE_DIBUJO, DE_TODOS);
        };
        return filas.stream()
                .map(f -> new CapaCatalogo(f.id(), f.nombre(), f.descripcion(), visibilidad(f.visibilidad()),
                        f.categoria(), (f.nombreDueno() + " " + f.apellidoDueno()).trim(), Boolean.TRUE.equals(f.favorita())))
                .toList();
    }

    private static Visibilidad visibilidad(String codigo) {
        return switch (codigo) {
            case PUBLICA -> Visibilidad.PUBLICA;
            case FORMAL -> Visibilidad.FORMAL;
            case AREA_DE_DIBUJO -> Visibilidad.AREA_DE_DIBUJO;
            default -> Visibilidad.PRIVADA;
        };
    }
}
