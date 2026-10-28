import { Provider } from '@angular/core';
import { MapaOpenLayers } from './adapter/out/openlayers/mapa-openlayers';
import { MapaPort } from './application/port/out/mapa.port';

/** El mapa se dibuja con OpenLayers; nadie fuera de este archivo conoce la implementación. */
export function proveerMapa(): Provider[] {
  return [{ provide: MapaPort, useExisting: MapaOpenLayers }];
}
