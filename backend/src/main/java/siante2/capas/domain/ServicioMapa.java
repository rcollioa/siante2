package siante2.capas.domain;

import java.util.List;

/** Lo que se dibuja de una capa por URL: el tipo de servidor y las capas que publica. Sin la URL del servidor: el
 * navegador pide las imágenes al backend, que es el único que habla con él. */
public record ServicioMapa(TipoServicio tipo, List<CapaDeServicio> capas) {
}
