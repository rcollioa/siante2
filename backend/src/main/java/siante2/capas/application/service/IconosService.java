package siante2.capas.application.service;

import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import siante2.capas.application.port.in.CrearIconoUseCase;
import siante2.capas.application.port.in.EditarIconoUseCase;
import siante2.capas.application.port.in.EliminarIconoUseCase;
import siante2.capas.application.port.in.ListarIconosUseCase;
import siante2.capas.application.port.out.IconosPort;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;
import siante2.compartido.domain.RecursoNoEncontradoException;
import siante2.compartido.domain.SolicitudInvalidaException;
import siante2.usuarios.application.port.in.IdentificarUsuarioUseCase;

@Service
public class IconosService implements ListarIconosUseCase, CrearIconoUseCase, EditarIconoUseCase, EliminarIconoUseCase {

    private static final Logger log = LoggerFactory.getLogger(IconosService.class);
    /** ID que ningún usuario tiene: sin registro en USERS solo se ven los del sistema. */
    private static final int SIN_USUARIO = -1;
    /** Los largos de NAME y NAME_IMAGE en POINTS_CATALOG. */
    private static final int MAXIMO_TEXTO = 200;
    /** Sobra para un PNG de 66 x 88, el tamaño máximo de SIATE, y cada elemento que lo use guarda una copia. */
    private static final int MAXIMO_IMAGEN = 32000;
    private static final List<String> PREFIJOS = List.of("data:image/png;base64,", "data:image/svg+xml;base64,");

    private final IconosPort iconos;
    private final IdentificarUsuarioUseCase usuarios;

    public IconosService(IconosPort iconos, IdentificarUsuarioUseCase usuarios) {
        this.iconos = iconos;
        this.usuarios = usuarios;
    }

    @Override
    public List<Icono> listar(String ivUser) {
        return iconos.listar(usuarios.idDe(ivUser).orElse(SIN_USUARIO));
    }

    @Override
    public Icono crear(String ivUser, DatosIcono datos) {
        DatosIcono validos = validados(datos);
        int dueno = usuarios.idDe(ivUser)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no está registrado."));
        int id = iconos.crear(dueno, validos);
        log.info("Ícono {} creado", id);
        return new Icono(id, validos.nombre(), validos.archivo(), validos.imagen(), true);
    }

    @Override
    public void editar(int id, String ivUser, DatosIcono datos) {
        DatosIcono validos = validados(datos);
        if (!iconos.actualizar(id, dueno(ivUser), validos)) {
            throw noExiste();
        }
        log.info("Ícono {} editado", id);
    }

    @Override
    public void eliminar(int id, String ivUser) {
        if (!iconos.eliminar(id, dueno(ivUser))) {
            throw noExiste();
        }
        log.info("Ícono {} eliminado", id);
    }

    private int dueno(String ivUser) {
        return usuarios.idDe(ivUser).orElseThrow(IconosService::noExiste);
    }

    private static RecursoNoEncontradoException noExiste() {
        return new RecursoNoEncontradoException("El ícono no existe.");
    }

    private static DatosIcono validados(DatosIcono datos) {
        String nombre = datos.nombre() == null ? "" : datos.nombre().trim();
        if (nombre.isEmpty()) {
            throw new SolicitudInvalidaException("Nombre requerido.");
        }
        if (nombre.length() > MAXIMO_TEXTO) {
            throw new SolicitudInvalidaException("El nombre no puede tener más de " + MAXIMO_TEXTO + " caracteres.");
        }
        String archivo = datos.archivo() == null ? "" : datos.archivo().trim();
        if (archivo.length() > MAXIMO_TEXTO) {
            throw new SolicitudInvalidaException("El nombre del archivo no puede tener más de " + MAXIMO_TEXTO + " caracteres.");
        }
        return new DatosIcono(nombre, archivo, imagenValida(datos.imagen()));
    }

    /** El tamaño en píxeles lo revisa el navegador, que es quien sabe leer un SVG. */
    private static String imagenValida(String imagen) {
        if (imagen == null || imagen.length() > MAXIMO_IMAGEN) {
            throw new SolicitudInvalidaException("La imagen del ícono falta o es demasiado grande.");
        }
        String prefijo = PREFIJOS.stream().filter(imagen::startsWith).findFirst()
                .orElseThrow(() -> new SolicitudInvalidaException("Los tipos de archivos permitidos son SVG y PNG."));
        try {
            Base64.getDecoder().decode(imagen.substring(prefijo.length()));
            return imagen;
        } catch (IllegalArgumentException e) {
            throw new SolicitudInvalidaException("Este tipo de archivo presenta problemas de lectura.");
        }
    }
}
