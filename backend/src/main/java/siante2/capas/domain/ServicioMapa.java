package siante2.capas.domain;

import java.util.List;

/** Lo que se dibuja de una capa por URL: el tipo de servidor y las capas que publica. Sin la URL del servidor: el
 * navegador pide las imágenes al backend, que es el único que habla con él. {@code disponible} es false si el servidor
 * no respondió al abrir la capa: como en SIATE, la capa se abre igual, vacía, y se avisa. {@code grilla}: la de los
 * mosaicos en un WMTS; null en los demás. */
public record ServicioMapa(TipoServicio tipo, List<CapaDeServicio> capas, boolean disponible, GrillaWmts grilla) {

    public ServicioMapa(TipoServicio tipo, List<CapaDeServicio> capas) {
        this(tipo, capas, true, null);
    }

    public static ServicioMapa wmts(List<CapaDeServicio> capas, GrillaWmts grilla) {
        return new ServicioMapa(TipoServicio.WMTS, capas, true, grilla);
    }

    public static ServicioMapa noDisponible(TipoServicio tipo) {
        return new ServicioMapa(tipo, List.of(), false, null);
    }
}
