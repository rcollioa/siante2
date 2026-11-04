package siante2.capas.domain;

import java.util.List;
import java.util.Objects;

/** Lo que el dueño dibujó, cambió y borró de una capa desde la última vez que guardó. En {@code nuevos} el {@code id} no
 * se usa, porque lo asigna el guardado. El {@code icono} del estilo no se edita todavía: se ignora al guardar. */
public record CambiosElementos(List<ElementoCapa> nuevos, List<ElementoCapa> modificados, List<Integer> eliminados) {

    public CambiosElementos {
        nuevos = Objects.requireNonNullElse(nuevos, List.of());
        modificados = Objects.requireNonNullElse(modificados, List.of());
        eliminados = Objects.requireNonNullElse(eliminados, List.of());
    }

    public int cantidad() {
        return nuevos.size() + modificados.size() + eliminados.size();
    }
}
