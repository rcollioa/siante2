package siante2.capas.domain;

/** Un nivel de zoom de un servidor WMTS (TileMatrix): la resolución en unidades del mapa por píxel, la esquina
 * superior izquierda de la grilla, el tamaño de cada mosaico en píxeles y cuántos mosaicos tiene. */
public record MatrizMosaicos(String id, double resolucion, double origenX, double origenY, int anchoMosaico,
                             int altoMosaico, int columnas, int filas) {
}
