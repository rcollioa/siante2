import { Injectable, signal } from '@angular/core';
import Feature from 'ol/Feature';
import Map from 'ol/Map';
import View from 'ol/View';
import { defaults as controlesPorDefecto, ScaleLine } from 'ol/control';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import { fromLonLat } from 'ol/proj';
import VectorSource from 'ol/source/Vector';
import { Circle, Fill, Stroke, Style } from 'ol/style';
import { CapaBase, crearCapasBase } from './capas-base';

const CENTRO_CHILE = fromLonLat([-70.65, -33.45]);
const ZOOM_DIRECCION = 17;

// El canvas no lee variables CSS: mismos valores que --color-danger y --color-surface.
const ESTILO_MARCADOR = new Style({
  image: new Circle({ radius: 8, fill: new Fill({ color: '#b3261e' }), stroke: new Stroke({ color: '#ffffff', width: 3 }) })
});

/** Instancia única del mapa; las funcionalidades agregan sus capas e interacciones aquí. */
@Injectable({ providedIn: 'root' })
export class MapaService {
  private readonly capasBase = crearCapasBase();
  private readonly capaBaseActual = signal<CapaBase>('mapa');
  readonly capaBase = this.capaBaseActual.asReadonly();

  private readonly marcador = new VectorSource();

  readonly mapa = new Map({
    layers: [...Object.values(this.capasBase), new VectorLayer({ source: this.marcador, style: ESTILO_MARCADOR })],
    view: new View({ center: CENTRO_CHILE, zoom: 4 }),
    controls: controlesPorDefecto({ attributionOptions: { collapsible: false } }).extend([new ScaleLine()])
  });

  usarCapaBase(capa: CapaBase): void {
    for (const [nombre, capaOl] of Object.entries(this.capasBase)) {
      capaOl.setVisible(nombre === capa);
    }
    this.capaBaseActual.set(capa);
  }

  /** Centra el mapa en el punto (EPSG:4326) y lo marca; sin animación si el usuario reduce el movimiento. */
  irA(longitud: number, latitud: number): void {
    const punto = fromLonLat([longitud, latitud]);
    this.marcador.clear();
    this.marcador.addFeature(new Feature(new Point(punto)));
    const sinMovimiento = globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    this.mapa.getView().animate({ center: punto, zoom: ZOOM_DIRECCION, duration: sinMovimiento ? 0 : 600 });
  }

  quitarMarcador(): void {
    this.marcador.clear();
  }

  montar(elemento: HTMLElement): void {
    this.mapa.setTarget(elemento);
  }

  desmontar(): void {
    this.mapa.setTarget(undefined);
  }
}
