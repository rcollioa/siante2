package siante2.capas.domain;

/** El servidor de mapas de una capa por URL, tal como lo registró su dueño en SIATE. Nunca sale de la API. */
public record OrigenServicio(TipoServicio tipo, String url) {
}
