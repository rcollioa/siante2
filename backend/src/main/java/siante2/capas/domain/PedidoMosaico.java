package siante2.capas.domain;

import siante2.compartido.domain.SolicitudInvalidaException;

/** Un mosaico WMTS (GetTile) que pide el navegador: la capa del servidor, el nivel (matriz) y la fila y columna. */
public record PedidoMosaico(String capa, String matriz, int fila, int columna) {

    public PedidoMosaico {
        if (capa == null || capa.isBlank() || matriz == null || matriz.isBlank() || fila < 0 || columna < 0) {
            throw new SolicitudInvalidaException("El mosaico pedido no es válido.");
        }
    }

    /** Si cae dentro de la matriz: un mosaico fuera de la grilla no existe en el servidor. */
    public boolean dentroDe(MatrizMosaicos matrizMosaicos) {
        return fila < matrizMosaicos.filas() && columna < matrizMosaicos.columnas();
    }
}
