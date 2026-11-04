package siante2.capas.domain;

/** Una capa nueva que muestra lo que publica un servidor de mapas. El tipo de servidor se deduce de la URL, como en SIATE. */
public record CapaPorUrl(String nombre, String descripcion, String url) {
}
