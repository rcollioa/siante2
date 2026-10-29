package siante2.capas.domain;

/** Una imagen que entregó un servidor de mapas, con su tipo (image/png, image/jpeg…). */
public record ImagenMapa(byte[] contenido, String tipo) {
}
