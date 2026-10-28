import { Provider } from '@angular/core';
import { CapasHttp } from './adapter/out/http/capas-http';
import { CapasRepositorio } from './application/port/out/capas.repositorio';

/** Las capas se leen de la API; nadie fuera de este archivo conoce la implementación. */
export function proveerCapas(): Provider[] {
  return [{ provide: CapasRepositorio, useExisting: CapasHttp }];
}
