package siante2.capas.application.service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import siante2.capas.application.port.in.GuardarElementosUseCase;
import siante2.capas.application.port.in.ImportarCapaUseCase;
import siante2.capas.application.port.out.ElementosWritePort;
import siante2.capas.domain.CambiosElementos;
import siante2.capas.domain.CapaImportada;
import siante2.capas.domain.CapaPropia;
import siante2.capas.domain.ElementoCapa;
import siante2.capas.domain.EstiloElemento;
import siante2.capas.domain.Metadato;
import siante2.capas.domain.Visibilidad;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;

/** Dibujar, editar e importar los elementos de una capa propia. SIATE no validaba nada de esto en el servidor. */
@Service
public class ElementosService implements GuardarElementosUseCase, ImportarCapaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ElementosService.class);
    /** Mucho más de lo que se dibuja a mano entre dos guardados; también respeta el límite de 1000 de un IN en Oracle. */
    private static final int MAXIMO_CAMBIOS = 1000;
    private static final int SIN_CATEGORIA = 0;
    /** Se insertan de una vez: lo que SIATE deja para su proceso nocturno (más de 1,8 MB de JSON) ronda esta cantidad. */
    private static final int MAXIMO_IMPORTADOS = 10000;
    /** Holgados: la descripción de un KML puede ser una tabla en HTML, y no debe impedir importarlo ni editarlo. */
    private static final int MAXIMO_NOMBRE = 500;
    private static final int MAXIMO_TEXTO = 65536;
    private static final int MAXIMO_METADATOS = 1000;
    private static final double MAXIMO_GROSOR = 1000;
    private static final Set<String> GEOMETRIAS =
            Set.of("Point", "LineString", "Polygon", "MultiPoint", "MultiLineString", "MultiPolygon");
    /** Los colores que SIATE guarda: hexadecimal, rgb(a) o el nombre CSS de un archivo importado. */
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{3,8}|rgba?\\([0-9., %/]{5,40}\\)|[a-zA-Z]{3,30}");

    private final ElementosWritePort escritura;
    private final IdentificarUsuarioUseCase usuarios;

    public ElementosService(ElementosWritePort escritura, IdentificarUsuarioUseCase usuarios) {
        this.escritura = escritura;
        this.usuarios = usuarios;
    }

    @Override
    public List<Integer> guardar(int capaId, String ivUser, CambiosElementos cambios) {
        validar(cambios);
        if (cambios.cantidad() == 0) {
            return List.of();
        }
        int dueno = usuarios.idDe(ivUser).orElseThrow(ElementosService::noExiste);
        List<Integer> ids = escritura.guardar(capaId, dueno, cambios).orElseThrow(ElementosService::noExiste);
        log.info("Capa {}: {} elementos nuevos, {} modificados y {} eliminados", capaId, cambios.nuevos().size(),
                cambios.modificados().size(), cambios.eliminados().size());
        return ids;
    }

    @Override
    public CapaPropia importar(String ivUser, CapaImportada capa) {
        CapaImportada valida = validada(capa);
        int dueno = usuarios.idDe(ivUser)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no está registrado."));
        int id = escritura.importar(dueno, valida);
        log.info("Capa {} importada de un archivo {} con {} elementos", id, valida.origen(), valida.elementos().size());
        return new CapaPropia(id, valida.nombre(), valida.descripcion(), Visibilidad.PRIVADA, SIN_CATEGORIA);
    }

    private static CapaImportada validada(CapaImportada capa) {
        String nombre = CapasService.nombreValido(capa.nombre());
        String descripcion = CapasService.descripcionValida(capa.descripcion());
        if (capa.origen() == null) {
            throw new SolicitudInvalidaException("El tipo de archivo no es válido.");
        }
        if (capa.elementos().isEmpty()) {
            throw new SolicitudInvalidaException("El archivo no tiene geometrías.");
        }
        if (capa.elementos().size() > MAXIMO_IMPORTADOS) {
            throw new SolicitudInvalidaException("No se pueden importar más de " + MAXIMO_IMPORTADOS + " elementos.");
        }
        capa.elementos().forEach(ElementosService::validar);
        return new CapaImportada(nombre, descripcion, capa.origen(), capa.elementos());
    }

    private static void validar(CambiosElementos cambios) {
        if (cambios.cantidad() > MAXIMO_CAMBIOS) {
            throw new SolicitudInvalidaException("No se pueden guardar más de " + MAXIMO_CAMBIOS + " cambios a la vez.");
        }
        cambios.nuevos().forEach(ElementosService::validar);
        cambios.modificados().forEach(ElementosService::validar);
        if (cambios.modificados().stream().anyMatch(elemento -> elemento.id() < 0)
                || cambios.eliminados().stream().anyMatch(id -> id == null || id < 0)) {
            throw new SolicitudInvalidaException("El identificador de un elemento no es válido.");
        }
    }

    private static void validar(ElementoCapa elemento) {
        if (elemento == null || elemento.geometria() == null || !GEOMETRIAS.contains(elemento.geometria().type())) {
            throw new SolicitudInvalidaException("La geometría de un elemento no es válida.");
        }
        if (elemento.radio() < 0 || elemento.radio() > 0 && !"Point".equals(elemento.geometria().type())) {
            throw new SolicitudInvalidaException("El radio de un elemento no es válido.");
        }
        exigirLargo(elemento.nombre(), MAXIMO_NOMBRE, "El nombre");
        exigirLargo(elemento.descripcion(), MAXIMO_TEXTO, "La descripción");
        validar(elemento.estilo());
        validar(elemento.metadatos());
    }

    private static void validar(EstiloElemento estilo) {
        if (estilo == null) {
            return;
        }
        if (!esColor(estilo.color()) || !esColor(estilo.relleno())) {
            throw new SolicitudInvalidaException("El color de un elemento no es válido.");
        }
        if (estilo.grosor() < 0 || estilo.grosor() > MAXIMO_GROSOR) {
            throw new SolicitudInvalidaException("El grosor de línea de un elemento no es válido.");
        }
    }

    private static void validar(List<Metadato> metadatos) {
        if (metadatos == null) {
            return;
        }
        if (metadatos.size() > MAXIMO_METADATOS) {
            throw new SolicitudInvalidaException("Un elemento no puede tener más de " + MAXIMO_METADATOS + " metadatos.");
        }
        for (Metadato metadato : metadatos) {
            exigirLargo(metadato.nombre(), MAXIMO_NOMBRE, "El nombre de un metadato");
            exigirLargo(metadato.valor(), MAXIMO_TEXTO, "La descripción de un metadato");
        }
    }

    private static boolean esColor(String color) {
        return color == null || color.isEmpty() || COLOR.matcher(color).matches();
    }

    private static void exigirLargo(String texto, int maximo, String campo) {
        if (texto != null && texto.length() > maximo) {
            throw new SolicitudInvalidaException(campo + " no puede tener más de " + maximo + " caracteres.");
        }
    }

    private static RecursoNoEncontradoException noExiste() {
        return new RecursoNoEncontradoException("La capa no existe o no se puede dibujar en ella.");
    }
}
