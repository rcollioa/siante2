import { Injectable } from '@angular/core';
import { LectorExcel } from '../../../application/port/out/lector-excel.port';

/** La librería se descarga solo al importar un Excel. */
@Injectable({ providedIn: 'root' })
export class LectorExcelNavegador extends LectorExcel {
  async leer(contenido: ArrayBuffer): Promise<string[][]> {
    const { readSheet } = await import('read-excel-file/browser');
    const filas = await readSheet(new Blob([contenido]));
    return filas.map((fila) => fila.map((celda) => (celda === null ? '' : String(celda))));
  }
}
