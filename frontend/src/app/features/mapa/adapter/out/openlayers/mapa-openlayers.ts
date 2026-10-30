import { Injectable, signal } from '@angular/core';
import Feature from 'ol/Feature';
import MapaOl from 'ol/Map';
import Overlay from 'ol/Overlay';
import View from 'ol/View';
import { defaults as controlesPorDefecto, ScaleLine } from 'ol/control';
import { Coordinate } from 'ol/coordinate';
import { EventsKey } from 'ol/events';
import { unByKey } from 'ol/Observable';
import LineString from 'ol/geom/LineString';
import Point from 'ol/geom/Point';
import Draw from 'ol/interaction/Draw';
import { Extent } from 'ol/extent';
import BaseLayer from 'ol/layer/Base';
import LayerGroup from 'ol/layer/Group';
import TileLayer from 'ol/layer/Tile';
import VectorLayer from 'ol/layer/Vector';
import { fromLonLat, toLonLat, transformExtent } from 'ol/proj';
import TileWMS from 'ol/source/TileWMS';
import VectorSource from 'ol/source/Vector';
import { getLength } from 'ol/sphere';
import WMTS from 'ol/source/WMTS';
import WMTSTileGrid from 'ol/tilegrid/WMTS';
import { Circle, Fill, Stroke, Style, Text as Texto } from 'ol/style';
import { esPantallaChica } from '../../../../../shared/pantalla/pantalla-chica';
import { MapaPort } from '../../../application/port/out/mapa.port';
import {
  AvisoUbicacion,
  CapaBase,
  CapaWms,
  CapaWmts,
  ElementoMapa,
  ElementoSeleccionado,
  formatearDistancia,
  Extension,
  PuntoWms,
  ZonaVisible
} from '../../../domain/mapa';
import { crearCapasBase } from './capas-base';
import { aFeatures, ELEMENTO } from './elementos-openlayers';

const CENTRO_CHILE = fromLonLat([-70.65, -33.45]);
const ZOOM_DIRECCION = 17;
const ZOOM_UBICACION = 12;
const ZOOM_UBICACION_PANTALLA_CHICA = 7;
const PERMISO_DENEGADO = 1; // GeolocationPositionError.PERMISSION_DENIED
// En equipos de oficina no hay GPS que aproveche alta precisión: la red responde en segundos y una posición
// de los últimos 10 minutos sirve para centrar el mapa, así el siguiente ingreso es inmediato.
const OPCIONES_UBICACION: PositionOptions = { enableHighAccuracy: false, timeout: 10_000, maximumAge: 10 * 60_000 };
/** Propiedad de cada Feature y de cada capa de OpenLayers con el ID de su capa. */
const CAPA = 'capa';
/** Propiedad de una capa WMS con su extensión en EPSG:3857, para acercarse a ella. */
const EXTENSION = 'extension';
/** Teselas fallidas seguidas antes de avisar: una suelta (borde del mundo, servidor lento) no es un corte. */
const FALLAS_PARA_AVISAR = 4;
/** Preferencia de cada usuario en su navegador; si el almacenamiento no está disponible, parte con el mapa. */
const CLAVE_CAPA_BASE = 'siante2.capaBase';

// El canvas no lee variables CSS: mismos valores que --color-danger y --color-surface.
const ESTILO_MARCADOR = new Style({
  image: new Circle({ radius: 8, fill: new Fill({ color: '#b3261e' }), stroke: new Stroke({ color: '#ffffff', width: 3 }) })
});

// El punto naranjo de SIATE para la posición del usuario, con su misma leyenda.
const ESTILO_UBICACION = new Style({
  image: new Circle({
    radius: 8,
    fill: new Fill({ color: 'rgba(255, 255, 255, 0.8)' }),
    stroke: new Stroke({ color: '#ed7d31', width: 3 })
  }),
  text: new Texto({
    text: 'Tú estás aquí',
    offsetY: -20,
    font: '600 13px "IBM Plex Sans", sans-serif',
    fill: new Fill({ color: '#1a2033' }),
    stroke: new Stroke({ color: '#ffffff', width: 3 })
  })
});

// La regla de SIATE: línea amarilla de 4 px.
const ESTILO_MEDICION = new Style({
  stroke: new Stroke({ color: '#ffcc33', width: 4 }),
  image: new Circle({ radius: 6, fill: new Fill({ color: '#ffcc33' }) })
});

/** Instancia única del mapa con OpenLayers; el resto de la aplicación llega a él por MapaPort. */
@Injectable({ providedIn: 'root' })
export class MapaOpenLayers extends MapaPort {
  private readonly capasBase = crearCapasBase();
  private readonly capaBaseActual = signal<CapaBase>('mapa');
  readonly capaBase = this.capaBaseActual.asReadonly();

  /** Una capa de OpenLayers por cada capa abierta, en el orden en que se abrieron (la última, encima). */
  private readonly grupoCapas = new LayerGroup();
  private readonly capas = new Map<number, BaseLayer>();
  private readonly sinRespuesta = signal<ReadonlySet<number>>(new Set());
  readonly capasSinRespuesta = this.sinRespuesta.asReadonly();
  private ajustePendiente: EventsKey | EventsKey[] | null = null;
  private readonly marcador = new VectorSource();
  private readonly ubicacion = new VectorSource();
  private ubicacionPedida = false;
  private direccionElegida = false;
  private posicionUsuario: Coordinate | null = null;
  private teselasFallidasSeguidas = 0;

  readonly avisoUbicacion = signal<AvisoUbicacion | null>(null);
  readonly ubicando = signal(false);
  readonly sinMapaBase = signal(false);
  readonly elementoSeleccionado = signal<ElementoSeleccionado | null>(null);
  readonly consultaWms = signal<readonly PuntoWms[]>([]);
  readonly midiendo = signal(false);
  private readonly mediciones = new VectorSource();
  private dibujoMedicion: Draw | null = null;
  /** Una etiqueta por línea medida, con su largo; la última, mientras se dibuja. */
  private readonly etiquetas: Overlay[] = [];
  private etiquetaEnCurso: Overlay | null = null;

  readonly mapa = new MapaOl({
    layers: [
      ...Object.values(this.capasBase),
      this.grupoCapas,
      new VectorLayer({ source: this.marcador, style: ESTILO_MARCADOR }),
      new VectorLayer({ source: this.ubicacion, style: ESTILO_UBICACION }),
      new VectorLayer({ source: this.mediciones, style: ESTILO_MEDICION })
    ],
    view: new View({ center: CENTRO_CHILE, zoom: 4 }),
    controls: controlesPorDefecto({ attributionOptions: { collapsible: false } }).extend([new ScaleLine()])
  });

  constructor() {
    super();
    for (const capa of Object.values(this.capasBase)) {
      const fuente = capa.getSource();
      fuente?.on('tileloadend', () => {
        this.teselasFallidasSeguidas = 0;
        this.sinMapaBase.set(false);
      });
      fuente?.on('tileloaderror', () => {
        if (++this.teselasFallidasSeguidas >= FALLAS_PARA_AVISAR) {
          this.sinMapaBase.set(true);
        }
      });
    }
    if (leerCapaBase() === 'satelital') {
      this.usarCapaBase('satelital');
    }
    // Como la herramienta de selección de SIATE: un clic muestra la información del elemento.
    this.mapa.on('singleclick', ({ pixel, coordinate }) => {
      // Con la regla activa, cada clic es un punto de la línea que se mide, como en SIATE.
      if (this.midiendo()) {
        return;
      }
      // Solo cuentan las capas abiertas y visibles: OpenLayers no busca en las que no dibuja.
      const feature = this.mapa.forEachFeatureAtPixel(pixel, (encontrada) => encontrada as Feature, {
        layerFilter: (capa) => capa instanceof VectorLayer && capa.get(CAPA) !== undefined,
        hitTolerance: 4
      });
      this.elementoSeleccionado.set(
        feature ? { capa: feature.get(CAPA) as number, elemento: feature.get(ELEMENTO) as ElementoMapa } : null
      );
      // Como en SIATE, un elemento propio tapa lo que haya debajo en las capas WMS.
      this.consultaWms.set(feature ? [] : this.puntosWms(coordinate));
    });
  }

  mostrarCapa(id: number, elementos: readonly ElementoMapa[]): void {
    const features = aFeatures(elementos);
    features.forEach((feature) => feature.set(CAPA, id));
    const existente = this.capas.get(id);
    if (existente instanceof VectorLayer) {
      this.deseleccionarDe(id);
      existente.getSource()?.clear();
      existente.getSource()?.addFeatures(features);
      return;
    }
    this.agregar(id, new VectorLayer({ source: new VectorSource({ features }), properties: { [CAPA]: id } }));
  }

  /** Mosaicos pedidos al backend; uno que no llega marca la capa como sin respuesta, y uno que llega la desmarca. */
  mostrarCapaWms(id: number, wms: CapaWms): void {
    const fuente = new TileWMS({ url: wms.url, params: { LAYERS: wms.capas.join(','), TILED: true } });
    fuente.on('tileloaderror', () => this.marcarSinRespuesta(id, true));
    fuente.on('tileloadend', () => this.marcarSinRespuesta(id, false));
    this.agregar(id, new TileLayer({ source: fuente, properties: { [CAPA]: id, [EXTENSION]: aExtensionDelMapa(wms.extension) } }));
  }

  /** OpenLayers arma el GetFeatureInfo del mosaico en que cayó el clic; de esa URL al backend salen los valores. */
  private puntosWms(coordenada: Coordinate): PuntoWms[] {
    const resolucion = this.mapa.getView().getResolution();
    if (resolucion === undefined) {
      return [];
    }
    return [...this.capas.entries()]
      .reverse()
      .flatMap(([id, capa]) => {
        const fuente = capa instanceof TileLayer && capa.getVisible() ? capa.getSource() : null;
        const url = fuente instanceof TileWMS ? fuente.getFeatureInfoUrl(coordenada, resolucion, 'EPSG:3857', {}) : undefined;
        return url ? [aPuntoWms(id, new URL(url, location.origin).searchParams)] : [];
      });
  }

  /** Como SIATE, todas las capas del servidor, una sobre otra; los mosaicos los pide al backend con la grilla que
   * éste leyó del servidor. Fuera de la grilla no hay mosaicos: no se piden. */
  mostrarCapaWmts(id: number, wmts: CapaWmts): void {
    const { crs, matrices } = wmts.grilla;
    const [primera] = matrices;
    const grilla = new WMTSTileGrid({
      extent: [
        primera.origenX,
        primera.origenY - primera.filas * primera.altoMosaico * primera.resolucion,
        primera.origenX + primera.columnas * primera.anchoMosaico * primera.resolucion,
        primera.origenY
      ],
      origins: matrices.map((m) => [m.origenX, m.origenY]),
      resolutions: matrices.map((m) => m.resolucion),
      matrixIds: matrices.map((m) => m.id),
      tileSizes: matrices.map((m) => [m.anchoMosaico, m.altoMosaico])
    });
    const capas = wmts.capas.map((capa) => {
      const fuente = new WMTS({
        url: `${wmts.url}/${encodeURIComponent(capa)}/{TileMatrix}/{TileRow}/{TileCol}`,
        requestEncoding: 'REST',
        layer: capa,
        matrixSet: crs,
        style: '',
        projection: crs,
        tileGrid: grilla,
        wrapX: false
      });
      fuente.on('tileloaderror', () => this.marcarSinRespuesta(id, true));
      fuente.on('tileloadend', () => this.marcarSinRespuesta(id, false));
      return new TileLayer({ source: fuente });
    });
    this.agregar(id, new LayerGroup({ layers: capas, properties: { [CAPA]: id, [EXTENSION]: aExtensionDelMapa(wmts.extension) } }));
  }

  /** Sobre las que ya están; si la capa ya estaba, ocupa su lugar. */
  private agregar(id: number, capa: BaseLayer): void {
    const anterior = this.capas.get(id);
    const capas = this.grupoCapas.getLayers();
    if (anterior) {
      this.deseleccionarDe(id);
      capas.setAt(capas.getArray().indexOf(anterior), capa);
    } else {
      capas.push(capa);
    }
    this.capas.set(id, capa);
  }

  private marcarSinRespuesta(id: number, sinRespuesta: boolean): void {
    if (this.sinRespuesta().has(id) === sinRespuesta) {
      return;
    }
    this.sinRespuesta.update((ids) => {
      const nuevos = new Set(ids);
      if (sinRespuesta) {
        nuevos.add(id);
      } else {
        nuevos.delete(id);
      }
      return nuevos;
    });
  }

  quitarCapa(id: number): void {
    const capa = this.capas.get(id);
    if (!capa) {
      return;
    }
    this.grupoCapas.getLayers().remove(capa);
    this.capas.delete(id);
    this.marcarSinRespuesta(id, false);
    this.deseleccionarDe(id);
    this.cancelarAjuste();
  }

  cambiarVisibilidad(id: number, visible: boolean): void {
    this.capas.get(id)?.setVisible(visible);
    if (!visible) {
      this.deseleccionarDe(id);
    }
  }

  acercarA(id: number): void {
    const extension = this.extensionDe(id);
    if (!extension) {
      return;
    }
    this.cancelarAjuste();
    // Más margen arriba: ahí están el buscador y el panel de la capa abierta, que taparían elementos.
    const ajustar = () =>
      this.mapa.getView().fit(extension, { padding: [170, 60, 60, 60], maxZoom: ZOOM_DIRECCION, duration: sinMovimiento() ? 0 : 600 });
    // Abierta desde otra página, el mapa todavía no tiene tamaño: se ajusta al montarse.
    if (this.mapa.getSize()) {
      ajustar();
    } else {
      this.ajustePendiente = this.mapa.once('change:size', ajustar);
    }
  }

  /** La de sus elementos en una capa vectorial; la que informó el servidor en una WMS. */
  private extensionDe(id: number): Extent | null {
    const capa = this.capas.get(id);
    if (capa instanceof VectorLayer) {
      const fuente = capa.getSource() as VectorSource | null;
      return fuente && !fuente.isEmpty() ? fuente.getExtent() : null;
    }
    return (capa?.get(EXTENSION) as Extent | undefined) ?? null;
  }

  deseleccionar(): void {
    this.elementoSeleccionado.set(null);
    this.consultaWms.set([]);
  }

  alternarRegla(): void {
    if (this.midiendo()) {
      this.apagarRegla();
      return;
    }
    this.deseleccionar();
    // Un clic agrega un punto y el doble clic termina la línea, como en SIATE.
    const dibujo = new Draw({ source: this.mediciones, type: 'LineString', style: ESTILO_MEDICION });
    dibujo.on('drawstart', ({ feature }) => {
      const etiqueta = this.nuevaEtiqueta();
      const linea = feature.getGeometry() as LineString;
      linea.on('change', () => {
        etiqueta.getElement()!.textContent = formatearDistancia(getLength(linea));
        etiqueta.setPosition(linea.getLastCoordinate());
      });
    });
    dibujo.on('drawend', () => {
      this.etiquetaEnCurso?.getElement()?.classList.add('medicion-terminada');
      this.etiquetaEnCurso = null;
    });
    dibujo.on('drawabort', () => this.quitarEtiquetaEnCurso());
    this.mapa.addInteraction(dibujo);
    this.dibujoMedicion = dibujo;
    this.midiendo.set(true);
  }

  cancelarMedicion(): void {
    this.dibujoMedicion?.abortDrawing();
    this.quitarEtiquetaEnCurso();
  }

  private apagarRegla(): void {
    if (this.dibujoMedicion) {
      this.mapa.removeInteraction(this.dibujoMedicion);
      this.dibujoMedicion = null;
    }
    this.etiquetas.forEach((etiqueta) => this.mapa.removeOverlay(etiqueta));
    this.etiquetas.length = 0;
    this.etiquetaEnCurso = null;
    this.mediciones.clear();
    this.midiendo.set(false);
  }

  /** El canvas no dibuja texto con fondo: la etiqueta es un elemento HTML sobre el mapa (estilos en styles.css). */
  private nuevaEtiqueta(): Overlay {
    const elemento = document.createElement('div');
    elemento.className = 'medicion';
    const etiqueta = new Overlay({ element: elemento, offset: [0, -12], positioning: 'bottom-center', stopEvent: false });
    this.mapa.addOverlay(etiqueta);
    this.etiquetas.push(etiqueta);
    this.etiquetaEnCurso = etiqueta;
    return etiqueta;
  }

  private quitarEtiquetaEnCurso(): void {
    if (this.etiquetaEnCurso) {
      this.mapa.removeOverlay(this.etiquetaEnCurso);
      this.etiquetas.splice(this.etiquetas.indexOf(this.etiquetaEnCurso), 1);
      this.etiquetaEnCurso = null;
    }
  }

  private deseleccionarDe(capa: number): void {
    if (this.elementoSeleccionado()?.capa === capa) {
      this.elementoSeleccionado.set(null);
    }
    if (this.consultaWms().some((punto) => punto.capa === capa)) {
      this.consultaWms.update((puntos) => puntos.filter((punto) => punto.capa !== capa));
    }
  }

  private cancelarAjuste(): void {
    if (this.ajustePendiente) {
      unByKey(this.ajustePendiente);
      this.ajustePendiente = null;
    }
  }

  usarCapaBase(capa: CapaBase): void {
    for (const [nombre, capaOl] of Object.entries(this.capasBase)) {
      capaOl.setVisible(nombre === capa);
    }
    this.capaBaseActual.set(capa);
    this.teselasFallidasSeguidas = 0;
    guardarCapaBase(capa);
  }

  descartarAvisoMapaBase(): void {
    this.sinMapaBase.set(false);
  }

  descartarAvisoUbicacion(): void {
    this.avisoUbicacion.set(null);
  }

  /** Sin animación si el usuario reduce el movimiento. */
  irA(longitud: number, latitud: number): void {
    const punto = fromLonLat([longitud, latitud]);
    this.direccionElegida = true;
    this.marcador.clear();
    this.marcador.addFeature(new Feature(new Point(punto)));
    this.animarHacia(punto, ZOOM_DIRECCION);
  }

  quitarMarcador(): void {
    this.marcador.clear();
  }

  zonaVisible(): ZonaVisible {
    const vista = this.mapa.getView();
    const [longitud, latitud] = toLonLat(vista.getCenter() ?? CENTRO_CHILE);
    return { latitud, longitud, zoom: vista.getZoom() ?? 0 };
  }

  /** Como SIATE al abrir el mapa: lo centra en la posición del usuario y la marca. Pide la posición una sola vez. */
  ubicarUsuario(): void {
    const geolocalizacion = globalThis.navigator?.geolocation;
    if (this.ubicacionPedida || !geolocalizacion) {
      return;
    }
    this.ubicacionPedida = true;
    geolocalizacion.getCurrentPosition(
      ({ coords }) => {
        this.marcarUbicacion(coords.longitude, coords.latitude);
        // Si mientras tanto buscó una dirección, no se le mueve el mapa.
        if (!this.direccionElegida) {
          this.mapa.getView().setCenter(this.posicionUsuario!);
          this.mapa.getView().setZoom(zoomUbicacion());
        }
      },
      // Al abrir, si el usuario negó el permiso no se le avisa: ya sabe por qué no aparece.
      (error) => this.avisoUbicacion.set(error.code === PERMISO_DENEGADO ? null : 'no-disponible'),
      OPCIONES_UBICACION
    );
  }

  /** Botón "Mi ubicación": vuelve a la posición ya conocida o la pide de nuevo (p. ej. si falló al abrir). */
  irAMiUbicacion(): void {
    if (this.posicionUsuario) {
      this.animarHacia(this.posicionUsuario, zoomUbicacion());
      return;
    }
    const geolocalizacion = globalThis.navigator?.geolocation;
    if (!geolocalizacion) {
      this.avisoUbicacion.set('no-disponible');
      return;
    }
    this.ubicando.set(true);
    geolocalizacion.getCurrentPosition(
      ({ coords }) => {
        this.ubicando.set(false);
        this.marcarUbicacion(coords.longitude, coords.latitude);
        this.animarHacia(this.posicionUsuario!, zoomUbicacion());
      },
      // Aquí sí se avisa el permiso negado: el usuario pidió su ubicación y necesita saber cómo habilitarla.
      (error) => {
        this.ubicando.set(false);
        this.avisoUbicacion.set(error.code === PERMISO_DENEGADO ? 'sin-permiso' : 'no-disponible');
      },
      OPCIONES_UBICACION
    );
  }

  private marcarUbicacion(longitud: number, latitud: number): void {
    this.posicionUsuario = fromLonLat([longitud, latitud]);
    this.avisoUbicacion.set(null);
    this.ubicacion.clear();
    this.ubicacion.addFeature(new Feature(new Point(this.posicionUsuario)));
  }

  private animarHacia(punto: Coordinate, zoom: number): void {
    this.mapa.getView().animate({ center: punto, zoom, duration: sinMovimiento() ? 0 : 600 });
  }

  montar(elemento: HTMLElement): void {
    this.mapa.setTarget(elemento);
  }

  desmontar(): void {
    this.mapa.setTarget(undefined);
  }
}

/** La extensión que informó el servidor, en grados, pasada al sistema del mapa para acercarse a la capa. */
function aExtensionDelMapa(extension: Extension | null): Extent | undefined {
  return extension
    ? transformExtent([extension.oeste, extension.sur, extension.este, extension.norte], 'EPSG:4326', 'EPSG:3857')
    : undefined;
}

/** WMS 1.3.0 (el de OpenLayers) usa CRS, I y J; 1.1.1, SRS, X e Y. */
function aPuntoWms(capa: number, parametros: URLSearchParams): PuntoWms {
  const valor = (...nombres: string[]) => nombres.map((nombre) => parametros.get(nombre)).find((v) => v !== null) ?? '';
  return {
    capa,
    capas: valor('QUERY_LAYERS', 'LAYERS').split(','),
    bbox: valor('BBOX'),
    ancho: Number(valor('WIDTH')),
    alto: Number(valor('HEIGHT')),
    crs: valor('CRS', 'SRS'),
    version: valor('VERSION'),
    i: Number(valor('I', 'X')),
    j: Number(valor('J', 'Y'))
  };
}

function zoomUbicacion(): number {
  return esPantallaChica() ? ZOOM_UBICACION_PANTALLA_CHICA : ZOOM_UBICACION;
}

/** El usuario pidió reducir el movimiento: el mapa salta sin animación. */
function sinMovimiento(): boolean {
  return globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}

// localStorage puede lanzar (navegación privada, sitio bloqueado): la preferencia es opcional.
function leerCapaBase(): string | null {
  try {
    return globalThis.localStorage?.getItem(CLAVE_CAPA_BASE) ?? null;
  } catch {
    return null;
  }
}

function guardarCapaBase(capa: CapaBase): void {
  try {
    globalThis.localStorage?.setItem(CLAVE_CAPA_BASE, capa);
  } catch {
    // Sin almacenamiento, la elección dura hasta recargar la página.
  }
}
