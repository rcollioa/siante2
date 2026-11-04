/** Leer un Excel requiere una librería, que solo conoce su adaptador. */
export abstract class LectorExcel {
  /** Las celdas de la primera hoja, como texto. */
  abstract leer(contenido: ArrayBuffer): Promise<string[][]>;
}
