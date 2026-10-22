import { Injectable, signal } from '@angular/core';
import Map from 'ol/Map';
import View from 'ol/View';
import { defaults as controlesPorDefecto, ScaleLine } from 'ol/control';
import { fromLonLat } from 'ol/proj';
import { CapaBase, crearCapasBase } from './capas-base';

const CENTRO_CHILE = fromLonLat([-70.65, -33.45]);

/** Instancia única del mapa; las funcionalidades agregan sus capas e interacciones aquí. */
@Injectable({ providedIn: 'root' })
export class MapaService {
  private readonly capasBase = crearCapasBase();
  private readonly capaBaseActual = signal<CapaBase>('mapa');
  readonly capaBase = this.capaBaseActual.asReadonly();

  readonly mapa = new Map({
    layers: Object.values(this.capasBase),
    view: new View({ center: CENTRO_CHILE, zoom: 4 }),
    controls: controlesPorDefecto({ attributionOptions: { collapsible: false } }).extend([new ScaleLine()])
  });

  usarCapaBase(capa: CapaBase): void {
    for (const [nombre, capaOl] of Object.entries(this.capasBase)) {
      capaOl.setVisible(nombre === capa);
    }
    this.capaBaseActual.set(capa);
  }

  montar(elemento: HTMLElement): void {
    this.mapa.setTarget(elemento);
  }

  desmontar(): void {
    this.mapa.setTarget(undefined);
  }
}
