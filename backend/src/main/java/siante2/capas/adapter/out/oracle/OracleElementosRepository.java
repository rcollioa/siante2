package siante2.capas.adapter.out.oracle;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import siante2.capas.application.port.out.ElementosWritePort;
import siante2.capas.domain.CambiosElementos;
import siante2.capas.domain.ElementoCapa;
import siante2.compartido.adapter.out.oracle.GeometriaAWkt;
import siante2.compartido.domain.SolicitudInvalidaException;

/** Escribe en {@code LAYERS_FEATURES} con SQL nativo, porque {@code SPATIAL_OBJECT} no está en la entidad: se calcula en
 * Oracle desde el WKT. El JSON se escribe como lo lee SIATE ({@link ElementoSiate}). */
@Repository
class OracleElementosRepository implements ElementosWritePort {

    private static final Logger log = LoggerFactory.getLogger(OracleElementosRepository.class);

    private static final String INSERTAR = "insert into LAYERS_FEATURES"
            + " (ID_FEATURE, ID_LAYER, TYPE, DATE_CREATE, DATE_MODIFY, JSON_METADATA, SPATIAL_OBJECT)"
            + " values (?, ?, ?, ?, ?, ?, " + GeometriaAWkt.SQL_ESCRIBIR + ")";
    private static final String INSERTAR_SIN_GEOMETRIA = "insert into LAYERS_FEATURES"
            + " (ID_FEATURE, ID_LAYER, TYPE, DATE_CREATE, DATE_MODIFY, JSON_METADATA) values (?, ?, ?, ?, ?, ?)";
    private static final String ACTUALIZAR = "update LAYERS_FEATURES set JSON_METADATA = ?, SPATIAL_OBJECT = "
            + GeometriaAWkt.SQL_ESCRIBIR + ", DATE_MODIFY = ? where ID_FEATURE = ? and ID_LAYER = ?";
    private static final String ELIMINAR = "delete from LAYERS_FEATURES where ID_FEATURE = ? and ID_LAYER = ?";

    private final CapaJpaRepository capas;
    private final ElementoCapaJpaRepository elementos;
    private final JdbcTemplate jdbc;

    OracleElementosRepository(CapaJpaRepository capas, ElementoCapaJpaRepository elementos, JdbcTemplate jdbc) {
        this.capas = capas;
        this.elementos = elementos;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Optional<List<Integer>> guardar(int capaId, int duenoId, CambiosElementos cambios) {
        if (capas.bloquearParaEditar(capaId, duenoId, OracleCapasRepository.ACTIVA, OracleCapasRepository.GESTIONABLES) == 0) {
            return Optional.empty();
        }
        String inicio = capas.inicioJson(capaId).orElse(null);
        if (!InicioJsonCapa.sePuedeDibujar(inicio)) {
            return Optional.empty();
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (elementos.ultimoId(capaId).isEmpty() && inicio != null && !inicio.isBlank()) {
            repartirColeccion(capaId, ahora);
        }
        eliminar(capaId, cambios.eliminados());
        actualizar(capaId, cambios.modificados(), ahora);
        return Optional.of(insertarNuevos(capaId, cambios.nuevos(), ahora));
    }

    /** Una capa sin filas muestra los elementos de su JSON, pero con la primera fila dejaría de hacerlo: como
     * {@code LayerService.migration} de SIATE, primero los pasa a filas. Conservan su posición como ID, la que ya conoce
     * el navegador, y el JSON queda como estaba. */
    private void repartirColeccion(int capaId, LocalDateTime ahora) {
        List<String> features = ElementoSiate.featuresDe(capas.jsonDe(capaId).orElse(""));
        List<FilaElemento> conGeometria = new ArrayList<>();
        List<FilaElemento> sinGeometria = new ArrayList<>();
        for (int id = 0; id < features.size(); id++) {
            String json = features.get(id);
            ElementoSiate.leer(id, json).ifPresent(elemento -> {
                Optional<String> wkt = wktSiSePuede(elemento);
                (wkt.isPresent() ? conGeometria : sinGeometria)
                        .add(new FilaElemento(elemento.id(), ElementoSiate.tipo(elemento), json, wkt.orElse(null)));
            });
        }
        insertar(capaId, conGeometria, ahora);
        insertarSinGeometria(capaId, sinGeometria, ahora);
        if (!sinGeometria.isEmpty()) {
            log.warn("Capa {}: {} de {} elementos quedaron sin SPATIAL_OBJECT porque su geometría no es válida",
                    capaId, sinGeometria.size(), features.size());
        }
    }

    /** Lo que SIATE ya guardó no se pierde porque su geometría no sirva para Oracle Spatial: SIATE lo dibuja desde el JSON,
     * aunque el análisis espacial no lo encuentre. Por ejemplo, un polígono con huecos, que SIATE guarda sin cerrar. */
    private static Optional<String> wktSiSePuede(ElementoCapa elemento) {
        try {
            return Optional.of(GeometriaAWkt.escribir(ElementoSiate.geometriaEspacial(elemento)));
        } catch (SolicitudInvalidaException e) {
            return Optional.empty();
        }
    }

    private void eliminar(int capaId, List<Integer> ids) {
        if (!ids.isEmpty()) {
            jdbc.batchUpdate(ELIMINAR, ids.stream().map(id -> new Object[] {id, capaId}).toList());
        }
    }

    /** Sobre el JSON que ya había. Si otra sesión borró el elemento, no se recrea: es un cambio que el usuario no vio. */
    private void actualizar(int capaId, List<ElementoCapa> modificados, LocalDateTime ahora) {
        if (modificados.isEmpty()) {
            return;
        }
        Map<Integer, String> actuales = elementos.buscarPorCapaEIds(capaId, modificados.stream().map(ElementoCapa::id).toList())
                .stream().collect(Collectors.toMap(FilaElementoCapa::id, FilaElementoCapa::json));
        List<FilaElemento> filas = new ArrayList<>();
        for (ElementoCapa elemento : modificados) {
            if (!actuales.containsKey(elemento.id())) {
                throw new SolicitudInvalidaException("Uno de los elementos ya no existe. Vuelva a abrir la capa.");
            }
            String json = ElementoSiate.escribir(capaId, elemento.id(), elemento, actuales.get(elemento.id()), ahora);
            filas.add(new FilaElemento(elemento.id(), ElementoSiate.tipo(elemento), json,
                    GeometriaAWkt.escribir(ElementoSiate.geometriaEspacial(elemento))));
        }
        enLote(ACTUALIZAR, filas, (sentencia, fila, conexion, textos) -> {
            sentencia.setClob(1, clob(conexion, fila.json(), textos));
            sentencia.setClob(2, clob(conexion, fila.wkt(), textos));
            sentencia.setTimestamp(3, Timestamp.valueOf(ahora));
            sentencia.setInt(4, fila.id());
            sentencia.setInt(5, capaId);
        });
    }

    /** SIATE numera cada capa desde 0 con el mayor ID + 1 ({@code setLayersFeatures}). */
    private List<Integer> insertarNuevos(int capaId, List<ElementoCapa> nuevos, LocalDateTime ahora) {
        int siguiente = elementos.ultimoId(capaId).map(ultimo -> ultimo + 1).orElse(0);
        List<FilaElemento> filas = new ArrayList<>();
        for (ElementoCapa elemento : nuevos) {
            int id = siguiente++;
            filas.add(new FilaElemento(id, ElementoSiate.tipo(elemento), ElementoSiate.escribir(capaId, id, elemento, null, ahora),
                    GeometriaAWkt.escribir(ElementoSiate.geometriaEspacial(elemento))));
        }
        insertar(capaId, filas, ahora);
        return filas.stream().map(FilaElemento::id).toList();
    }

    private void insertar(int capaId, List<FilaElemento> filas, LocalDateTime ahora) {
        enLote(INSERTAR, filas, (sentencia, fila, conexion, textos) -> {
            datosDeInsercion(sentencia, fila, capaId, ahora, conexion, textos);
            sentencia.setClob(7, clob(conexion, fila.wkt(), textos));
        });
    }

    private void insertarSinGeometria(int capaId, List<FilaElemento> filas, LocalDateTime ahora) {
        enLote(INSERTAR_SIN_GEOMETRIA, filas,
                (sentencia, fila, conexion, textos) -> datosDeInsercion(sentencia, fila, capaId, ahora, conexion, textos));
    }

    private static void datosDeInsercion(PreparedStatement sentencia, FilaElemento fila, int capaId, LocalDateTime ahora,
                                         Connection conexion, List<Clob> textos) throws SQLException {
        sentencia.setInt(1, fila.id());
        sentencia.setInt(2, capaId);
        sentencia.setString(3, fila.tipo());
        sentencia.setTimestamp(4, Timestamp.valueOf(ahora));
        sentencia.setTimestamp(5, Timestamp.valueOf(ahora));
        sentencia.setClob(6, clob(conexion, fila.json(), textos));
    }

    private void enLote(String sql, List<FilaElemento> filas, Parametros parametros) {
        if (filas.isEmpty()) {
            return;
        }
        jdbc.execute((ConnectionCallback<Void>) conexion -> {
            List<Clob> textos = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
                for (FilaElemento fila : filas) {
                    parametros.asignar(sentencia, fila, conexion, textos);
                    sentencia.addBatch();
                }
                sentencia.executeBatch();
            } finally {
                for (Clob texto : textos) {
                    texto.free();
                }
            }
            return null;
        });
    }

    /** Como CLOB y no como texto: un WKT de más de 4000 bytes no cabe en el VARCHAR2 de una función de SQL. */
    private static Clob clob(Connection conexion, String texto, List<Clob> textos) throws SQLException {
        Clob clob = conexion.createClob();
        clob.setString(1, texto);
        textos.add(clob);
        return clob;
    }

    private record FilaElemento(int id, String tipo, String json, String wkt) {
    }

    @FunctionalInterface
    private interface Parametros {
        void asignar(PreparedStatement sentencia, FilaElemento fila, Connection conexion, List<Clob> textos) throws SQLException;
    }
}
