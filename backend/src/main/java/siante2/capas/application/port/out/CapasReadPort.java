package siante2.capas.application.port.out;

import java.util.List;
import java.util.Optional;
import siante2.capas.domain.Capa;
import siante2.capas.domain.ResumenCapa;

public interface CapasReadPort {

    /** Capas activas del usuario sin las áreas de dibujo de los grupos, como "Mis capas" en SIATE. */
    List<ResumenCapa> listarDe(int usuarioId);

    /** La capa activa si es del usuario, pública o formal; vacío si no existe o no puede verla. */
    Optional<Capa> buscarVisible(int capaId, int usuarioId);
}
