package siante2.capas.application.port.out;

import java.util.List;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;

/** Cada cambio exige que el ícono sea del dueño y esté activo; devuelve false si no. */
public interface IconosPort {

    /** Los activos del sistema y del usuario que tienen una imagen {@code data:}, en orden de creación. */
    List<Icono> listar(int usuarioId);

    /** Devuelve su ID, que asigna la base. */
    int crear(int duenoId, DatosIcono datos);

    boolean actualizar(int id, int duenoId, DatosIcono datos);

    boolean eliminar(int id, int duenoId);
}
