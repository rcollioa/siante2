import { Provider } from '@angular/core';
import { LectorExcelNavegador } from './adapter/out/excel/lector-excel-navegador';
import { CapasHttp } from './adapter/out/http/capas-http';
import { IconosHttp } from './adapter/out/http/iconos-http';
import { CapasRepositorio } from './application/port/out/capas.repositorio';
import { IconosRepositorio } from './application/port/out/iconos.repositorio';
import { LectorExcel } from './application/port/out/lector-excel.port';

/** Las capas se leen de la API; nadie fuera de este archivo conoce la implementación. */
export function proveerCapas(): Provider[] {
  return [
    { provide: CapasRepositorio, useExisting: CapasHttp },
    { provide: IconosRepositorio, useExisting: IconosHttp },
    { provide: LectorExcel, useExisting: LectorExcelNavegador }
  ];
}
