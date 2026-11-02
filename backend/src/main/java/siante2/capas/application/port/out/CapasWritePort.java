package siante2.capas.application.port.out;

import siante2.capas.domain.EdicionCapa;
import siante2.capas.domain.Visibilidad;

/** Cada escritura exige que la capa sea del dueño, esté activa y sea privada o pública: las formales y las áreas de
 * dibujo no se gestionan aquí. Devuelve false si ninguna capa cumple. */
public interface CapasWritePort {

    boolean actualizar(int capaId, int duenoId, EdicionCapa edicion);

    boolean cambiarVisibilidad(int capaId, int duenoId, Visibilidad visibilidad);

    /** Como SIATE, la deja inactiva sin borrarla: sus elementos y los grupos que la incluyen quedan intactos. */
    boolean eliminar(int capaId, int duenoId);
}
