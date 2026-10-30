package siante2.capas.adapter.out.wms;

import java.util.Objects;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.stereotype.Component;
import siante2.capas.application.port.out.ServiciosWmtsPort;
import siante2.capas.domain.ImagenMapa;
import siante2.capas.domain.PedidoMosaico;
import siante2.capas.domain.ServicioMapa;
import siante2.compartido.domain.ServicioExternoNoDisponibleException;

/** Habla con los servidores WMTS de las capas por URL. SIATE pedía el GetCapabilities desde el navegador cada vez que
 * se abría la capa; aquí lo pide el backend y queda en caché, porque cada mosaico necesita saber cómo pedirse. */
@Component
class ClienteWmts implements ServiciosWmtsPort {

    /** Como las imágenes WMS: un mapa pide decenas de mosaicos a la vez. */
    private static final int MOSAICOS_A_LA_VEZ = 32;

    private final ServidoresMapa servidores;
    private final Cache capacidades;

    ClienteWmts(ServidoresMapa servidores, CacheManager caches) {
        this.servidores = servidores;
        this.capacidades = Objects.requireNonNull(caches.getCache("capacidades-wmts"), "Falta el caché capacidades-wmts");
    }

    @Override
    public ServicioMapa servicioWmts(String url) {
        CapacidadesWmts.Leidas leidas = leidas(url);
        return leidas.grillaDelMapa()
                .filter(grilla -> !leidas.capasDe(grilla).isEmpty())
                .map(grilla -> ServicioMapa.wmts(leidas.capasDeServicio(grilla), leidas.grillaWmts(grilla)))
                .orElseThrow(() -> new ServicioExternoNoDisponibleException(
                        "El servidor de mapas de la capa no publica capas en un sistema de coordenadas que el mapa dibuje."));
    }

    /** Quien llama ya validó la capa y la matriz contra {@link #servicioWmts}. */
    @Override
    @ConcurrencyLimit(MOSAICOS_A_LA_VEZ)
    public ImagenMapa mosaicoWmts(String url, PedidoMosaico pedido) {
        CapacidadesWmts.Leidas leidas = leidas(url);
        CapacidadesWmts.Grilla grilla = leidas.grillaDelMapa()
                .orElseThrow(() -> new ServicioExternoNoDisponibleException("El servidor de mapas de la capa cambió."));
        CapacidadesWmts.Capa capa = leidas.capa(pedido.capa())
                .orElseThrow(() -> new ServicioExternoNoDisponibleException("El servidor de mapas de la capa cambió."));
        return servidores.imagen(UrlWmts.mosaico(url, leidas, capa, grilla.id(), pedido));
    }

    private CapacidadesWmts.Leidas leidas(String url) {
        try {
            return capacidades.get(url, () -> servidores.capacidades(UrlWmts.capacidades(url), CapacidadesWmts::leer));
        } catch (Cache.ValueRetrievalException e) {
            // El caché envuelve la excepción de quien carga el valor: se entrega la original.
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw e;
        }
    }
}
