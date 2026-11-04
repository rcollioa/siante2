package siante2.capas.application.port.out;

import java.util.List;
import java.util.Optional;
import siante2.capas.domain.Capa;
import siante2.capas.domain.CapaCatalogo;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.OrigenServicio;
import siante2.capas.domain.ResumenCapa;
import siante2.capas.domain.SeccionCatalogo;

public interface CapasReadPort {

    /** Capas activas del usuario sin las áreas de dibujo de los grupos, como "Mis capas" en SIATE. */
    List<CapaPropia> listarDe(int usuarioId);

    /** La capa activa si es del usuario, pública o formal; vacío si no existe o no puede verla. */
    Optional<Capa> buscarVisible(int capaId, int usuarioId);

    /** El servidor de mapas de una capa por URL que el usuario puede ver; vacío si no existe, no la puede ver o no es
     * por URL. */
    Optional<OrigenServicio> servicioVisible(int capaId, int usuarioId);

    /** Capas activas de los grupos que el usuario marcó como favoritos ({@code FAVORITES}), sin áreas de dibujo y
     * solo las que todavía puede ver: una que su dueño volvió privada deja de aparecer. */
    List<ResumenCapa> listarFavoritasDe(int usuarioId);

    /** Capas activas de una pestaña del catálogo, sin áreas de dibujo, con su marca de favorita para el usuario. */
    List<CapaCatalogo> catalogo(SeccionCatalogo seccion, int usuarioId);
}
