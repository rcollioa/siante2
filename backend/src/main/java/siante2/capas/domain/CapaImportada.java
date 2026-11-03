package siante2.capas.domain;

import java.util.List;
import java.util.Objects;

/** Una capa nueva con los elementos que el navegador leyó de un archivo. El {@code id} de los elementos no se usa. */
public record CapaImportada(String nombre, String descripcion, OrigenArchivo origen, List<ElementoCapa> elementos) {

    public CapaImportada {
        elementos = Objects.requireNonNullElse(elementos, List.of());
    }
}
