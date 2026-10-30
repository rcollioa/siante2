package siante2.capas.domain;

import java.util.List;

/** Lo que se dibuja de una capa por URL: el tipo de servidor y las capas que publica. Sin la URL del servidor: el
 * navegador pide las imágenes al backend, que es el único que habla con él. {@code disponible} es false si el servidor
 * no respondió al abrir la capa: como en SIATE, la capa se abre igual, vacía, y se avisa. */
public record ServicioMapa(TipoServicio tipo, List<CapaDeServicio> capas, boolean disponible) {

    public ServicioMapa(TipoServicio tipo, List<CapaDeServicio> capas) {
        this(tipo, capas, true);
    }

    public static ServicioMapa noDisponible(TipoServicio tipo) {
        return new ServicioMapa(tipo, List.of(), false);
    }
}
