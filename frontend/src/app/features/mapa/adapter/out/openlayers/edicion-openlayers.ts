import Collection from 'ol/Collection';
import Feature from 'ol/Feature';
import MapaOl from 'ol/Map';
import { equals } from 'ol/coordinate';
import Geometry from 'ol/geom/Geometry';
import DoubleClickZoom from 'ol/interaction/DoubleClickZoom';
import Draw, { createBox } from 'ol/interaction/Draw';
import Modify from 'ol/interaction/Modify';
import Snap from 'ol/interaction/Snap';
import Translate from 'ol/interaction/Translate';
import VectorSource from 'ol/source/Vector';
import { ElementoMapa, HerramientaDibujo, OyenteEdicion } from '../../../domain/mapa';
import { ELEMENTO, estiloDe, estiloResaltado, formaDe } from './elementos-openlayers';

/** Las herramientas de SIATE sobre una capa abierta. Lo seleccionado se mueve y cambia de forma; solo eso, para que un
 * arrastre para mover el mapa no deforme otro elemento, como pasaba en SIATE al dibujar. */
export class EdicionOpenLayers {
  private readonly modificables = new Collection<Feature>();
  private readonly trasladar = new Translate({ features: this.modificables, hitTolerance: 4 });
  private readonly modificar = new Modify({ features: this.modificables });
  private readonly ajustar: Snap;
  private dibujo: Draw | null = null;
  private seleccionada: Feature | null = null;

  constructor(
    private readonly mapa: MapaOl,
    readonly capa: number,
    fuente: VectorSource,
    private readonly oyente: OyenteEdicion
  ) {
    this.ajustar = new Snap({ source: fuente });
    this.trasladar.on('translateend', ({ features, coordinate, startCoordinate }) => {
      // Un clic sin arrastrar también termina un traslado.
      if (!equals(coordinate, startCoordinate)) {
        features.forEach((feature) => this.avisarCambio(feature as Feature));
      }
    });
    this.modificar.on('modifyend', ({ features }) => features.forEach((feature) => this.avisarCambio(feature as Feature)));
    // El orden importa: la última recibe primero los eventos. Un vértice gana a mover el elemento, y Snap, a todas.
    mapa.addInteraction(this.trasladar);
    mapa.addInteraction(this.modificar);
    mapa.addInteraction(this.ajustar);
  }

  get dibujando(): boolean {
    return this.dibujo !== null;
  }

  usarHerramienta(herramienta: HerramientaDibujo | null): void {
    if (this.dibujo) {
      this.mapa.removeInteraction(this.dibujo);
      this.dibujo = null;
    }
    this.permitirZoomConDobleClic(herramienta === null);
    this.cursor(herramienta === null ? '' : 'crosshair');
    if (herramienta === null) {
      this.actualizarModificables();
      return;
    }
    this.modificables.clear();
    this.dibujo = crearDibujo(herramienta);
    this.dibujo.on('drawend', ({ feature }) => {
      const forma = formaDe(feature.getGeometry() as Geometry);
      if (forma) {
        this.oyente.alDibujar(forma);
      }
    });
    this.mapa.removeInteraction(this.ajustar);
    this.mapa.addInteraction(this.dibujo);
    this.mapa.addInteraction(this.ajustar);
  }

  seleccionar(feature: Feature | null): void {
    const anterior = this.seleccionada;
    this.seleccionada = feature;
    if (anterior) {
      this.redibujar(anterior);
    }
    if (feature) {
      this.redibujar(feature);
    }
    this.actualizarModificables();
  }

  redibujar(feature: Feature): void {
    const elemento = feature.get(ELEMENTO) as ElementoMapa;
    feature.setStyle(feature === this.seleccionada ? estiloResaltado(elemento) : estiloDe(elemento));
  }

  terminar(): void {
    this.seleccionar(null);
    this.usarHerramienta(null);
    this.mapa.removeInteraction(this.trasladar);
    this.mapa.removeInteraction(this.modificar);
    this.mapa.removeInteraction(this.ajustar);
  }

  private actualizarModificables(): void {
    this.modificables.clear();
    if (this.seleccionada && !this.dibujo) {
      this.modificables.push(this.seleccionada);
    }
  }

  private avisarCambio(feature: Feature): void {
    const forma = formaDe(feature.getGeometry() as Geometry);
    if (forma) {
      this.oyente.alCambiarForma(feature.get(ELEMENTO) as ElementoMapa, forma);
    }
  }

  /** El doble clic que termina una línea o un polígono no debe además acercar el mapa. */
  private permitirZoomConDobleClic(permitir: boolean): void {
    this.mapa
      .getInteractions()
      .getArray()
      .filter((interaccion) => interaccion instanceof DoubleClickZoom)
      .forEach((interaccion) => interaccion.setActive(permitir));
  }

  private cursor(cursor: string): void {
    const lienzo = this.mapa.getTargetElement();
    if (lienzo) {
      lienzo.style.cursor = cursor;
    }
  }
}

function crearDibujo(herramienta: HerramientaDibujo): Draw {
  return herramienta === 'Box' ? new Draw({ type: 'Circle', geometryFunction: createBox() }) : new Draw({ type: herramienta });
}
