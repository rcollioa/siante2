package siante2.capas.adapter.out.oracle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.capas.application.port.out.CapasReadPort;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;
import siante2.capas.domain.TipoCapa;
import siante2.capas.domain.Visibilidad;

@Repository
@Transactional(readOnly = true)
class OracleCapasRepository implements CapasReadPort {

    private static final Logger log = LoggerFactory.getLogger(OracleCapasRepository.class);

    /** Códigos de SIATE ({@code MessageConstants}): {@code STATUS} y {@code VISIBILITY}. */
    static final String ACTIVA = "1";
    static final String PUBLICA = "1";
    static final String FORMAL = "2";
    static final String AREA_DE_DIBUJO = "3";

    /** SIATE guarda una capa por URL como {@code {"type":"WMS","url":...}}. */
    private static final Pattern POR_URL = Pattern.compile("\"type\"\\s*:\\s*\"(WMS|WFS|WMTS)\"", Pattern.CASE_INSENSITIVE);

    /** Visibilidades que ve cualquier usuario, además de las propias. */
    private static final List<String> DE_TODOS = List.of(PUBLICA, FORMAL);

    private final VistaCapaJpaRepository vista;
    private final ElementoCapaJpaRepository elementos;
    private final int usuarioCapasFormales;

    /** @param usuarioCapasFormales ID en USERS del usuario de sistema "SIANTE", dueño de las capas formales
     *     ({@code Config.IDSIANTE} del frontend de SIATE). */
    OracleCapasRepository(VistaCapaJpaRepository vista, ElementoCapaJpaRepository elementos,
                          @Value("${app.capas.usuario-formales:1}") int usuarioCapasFormales) {
        this.vista = vista;
        this.elementos = elementos;
        this.usuarioCapasFormales = usuarioCapasFormales;
    }

    @Override
    public List<ResumenCapa> listarDe(int usuarioId) {
        return vista.buscarDe(usuarioId, ACTIVA, AREA_DE_DIBUJO).stream()
                .map(f -> new ResumenCapa(f.id(), f.nombre(), f.descripcion(), visibilidad(f.visibilidad())))
                .toList();
    }

    @Override
    public Optional<Capa> buscarVisible(int capaId, int usuarioId) {
        return vista.buscarVisible(capaId, usuarioId, ACTIVA, DE_TODOS).map(f -> {
            TipoCapa tipo = f.inicioJson() != null && POR_URL.matcher(f.inicioJson()).find() ? TipoCapa.URL : TipoCapa.VECTORIAL;
            return new Capa(f.id(), f.nombre(), f.descripcion(), visibilidad(f.visibilidad()),
                    Objects.equals(f.dueno(), usuarioId), tipo, tipo == TipoCapa.URL ? List.of() : elementosDe(capaId));
        });
    }

    /** Un elemento que no se puede leer se omite: el resto de la capa se muestra igual. */
    private List<ElementoCapa> elementosDe(int capaId) {
        List<FilaElementoCapa> filas = elementos.buscarPorCapa(capaId);
        List<ElementoCapa> leidos = filas.stream()
                .map(fila -> ElementoSiate.leer(fila.id(), fila.json()))
                .flatMap(Optional::stream)
                .toList();
        if (leidos.size() < filas.size()) {
            log.warn("Capa {}: se omitieron {} de {} elementos que no tienen una geometría válida",
                    capaId, filas.size() - leidos.size(), filas.size());
        }
        return leidos;
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
