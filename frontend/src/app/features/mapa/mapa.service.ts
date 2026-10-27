import { Injectable, signal } from '@angular/core';
import Feature from 'ol/Feature';
import Map from 'ol/Map';
import View from 'ol/View';
import { defaults as controlesPorDefecto, ScaleLine } from 'ol/control';
import { Coordinate } from 'ol/coordinate';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import { fromLonLat, toLonLat } from 'ol/proj';
import VectorSource from 'ol/source/Vector';
import { Circle, Fill, Stroke, Style, Text as Texto } from 'ol/style';
import { esPantallaChica } from '../../shared/pantalla/pantalla-chica';
import { CapaBase, crearCapasBase } from './capas-base';

const CENTRO_CHILE = fromLonLat([-70.65, -33.45]);
const ZOOM_DIRECCION = 17;
const ZOOM_UBICACION = 12;
const ZOOM_UBICACION_PANTALLA_CHICA = 7;
const PERMISO_DENEGADO = 1; // GeolocationPositionError.PERMISSION_DENIED
// En equipos de oficina no hay GPS que aproveche alta precisión: la red responde en segundos y una posición
// de los últimos 10 minutos sirve para centrar el mapa, así el siguiente ingreso es inmediato.
const OPCIONES_UBICACION: PositionOptions = { enableHighAccuracy: false, timeout: 10_000, maximumAge: 10 * 60_000 };
/** Teselas fallidas seguidas antes de avisar: una suelta (borde del mundo, servidor lento) no es un corte. */
const FALLAS_PARA_AVISAR = 4;
/** Preferencia de cada usuario en su navegador; si el almacenamiento no está disponible, parte con el mapa. */
const CLAVE_CAPA_BASE = 'siante2.capaBase';

/** Por qué no se muestra la posición del usuario. */
export type AvisoUbicacion = 'no-disponible' | 'sin-permiso';

/** Centro (EPSG:4326) y zoom del mapa. */
export interface ZonaVisible {
  latitud: number;
  longitud: number;
  zoom: number;
}

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

/** Instancia única del mapa; las funcionalidades agregan sus capas e interacciones aquí. */
@Injectable({ providedIn: 'root' })
export class MapaService {
  private readonly capasBase = crearCapasBase();
  private readonly capaBaseActual = signal<CapaBase>('mapa');
  readonly capaBase = this.capaBaseActual.asReadonly();

  private readonly marcador = new VectorSource();
  private readonly ubicacion = new VectorSource();
  private ubicacionPedida = false;
  private direccionElegida = false;
  private posicionUsuario: Coordinate | null = null;
  private teselasFallidasSeguidas = 0;

  /** Por qué no aparece la posición del usuario, para avisarle; null si no hay nada que avisar. */
  readonly avisoUbicacion = signal<AvisoUbicacion | null>(null);
  /** Esperando la posición que pidió el usuario con "Mi ubicación". */
  readonly ubicando = signal(false);
  /** Las teselas de la capa base no cargan: sin salida a internet o servidor de mapas caído. */
  readonly sinMapaBase = signal(false);

  readonly mapa = new Map({
    layers: [
      ...Object.values(this.capasBase),
      new VectorLayer({ source: this.marcador, style: ESTILO_MARCADOR }),
      new VectorLayer({ source: this.ubicacion, style: ESTILO_UBICACION })
    ],
    view: new View({ center: CENTRO_CHILE, zoom: 4 }),
    controls: controlesPorDefecto({ attributionOptions: { collapsible: false } }).extend([new ScaleLine()])
  });

  constructor() {
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
  }

  usarCapaBase(capa: CapaBase): void {
    for (const [nombre, capaOl] of Object.entries(this.capasBase)) {
      capaOl.setVisible(nombre === capa);
    }
    this.capaBaseActual.set(capa);
    this.teselasFallidasSeguidas = 0;
    guardarCapaBase(capa);
  }

  /** Centra el mapa en el punto (EPSG:4326) y lo marca; sin animación si el usuario reduce el movimiento. */
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

  /** Lo que se está mirando: las sugerencias de direcciones cercanas aparecen primero. */
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
    const sinMovimiento = globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    this.mapa.getView().animate({ center: punto, zoom, duration: sinMovimiento ? 0 : 600 });
  }

  montar(elemento: HTMLElement): void {
    this.mapa.setTarget(elemento);
  }

  desmontar(): void {
    this.mapa.setTarget(undefined);
  }
}

function zoomUbicacion(): number {
  return esPantallaChica() ? ZOOM_UBICACION_PANTALLA_CHICA : ZOOM_UBICACION;
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
