package siante2.capas.domain;

import java.util.List;

/** Un elemento que el servidor de mapas encontró en el punto consultado: la capa del servidor en que está (su
 * título, o vacío si el servidor no lo dice) y sus datos, en el orden en que los entregó. */
public record ElementoDeServicio(String capa, List<Metadato> datos) {

    public ElementoDeServicio {
        datos = List.copyOf(datos);
    }

    public ElementoDeServicio conCapa(String titulo) {
        return new ElementoDeServicio(titulo, datos);
    }
}
