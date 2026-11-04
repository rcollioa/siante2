package siante2.capas.adapter.out.oracle;

import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.capas.application.port.out.IconosPort;
import siante2.capas.domain.DatosIcono;
import siante2.capas.domain.Icono;

@Repository
@Transactional(readOnly = true)
class OracleIconosRepository implements IconosPort {

    /** El dueño de los íconos del sistema en SIATE. */
    private static final int SISTEMA = 1;
    private static final String ACTIVO = "1";
    private static final String INACTIVO = "0";
    /** Los íconos por URL de SIATE no se ofrecen: la CSP impide cargar imágenes de otros servidores. */
    private static final String IMAGEN = "data:image/";

    private final IconoJpaRepository iconos;

    OracleIconosRepository(IconoJpaRepository iconos) {
        this.iconos = iconos;
    }

    @Override
    public List<Icono> listar(int usuarioId) {
        return iconos.listar(usuarioId, SISTEMA, ACTIVO).stream()
                .filter(fila -> fila.imagen() != null && fila.imagen().startsWith(IMAGEN))
                .map(fila -> new Icono(fila.id(), fila.nombre(), fila.archivo(), fila.imagen(),
                        fila.dueno() == usuarioId && usuarioId != SISTEMA))
                .toList();
    }

    @Override
    @Transactional
    public int crear(int duenoId, DatosIcono datos) {
        return iconos.save(new PuntoCatalogoEntity(duenoId, datos.nombre(), datos.archivo(), datos.imagen(), ACTIVO)).getId();
    }

    @Override
    @Transactional
    public boolean actualizar(int id, int duenoId, DatosIcono datos) {
        return iconos.actualizar(id, duenoId, datos.nombre(), datos.archivo(), datos.imagen(), ACTIVO) > 0;
    }

    @Override
    @Transactional
    public boolean eliminar(int id, int duenoId) {
        return iconos.desactivar(id, duenoId, INACTIVO, ACTIVO) > 0;
    }
}
