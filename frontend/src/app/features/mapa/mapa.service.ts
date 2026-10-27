import { Injectable, signal } from '@angular/core';
import Feature from 'ol/Feature';
import Map from 'ol/Map';
import View from 'ol/View';
import { defaults as controlesPorDefecto, ScaleLine } from 'ol/control';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import { fromLonLat } from 'ol/proj';
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
  /** La posición no se pudo obtener por un motivo distinto a que el usuario la negara. */
  readonly sinUbicacion = signal(false);

  readonly mapa = new Map({
    layers: [
      ...Object.values(this.capasBase),
      new VectorLayer({ source: this.marcador, style: ESTILO_MARCADOR }),
      new VectorLayer({ source: this.ubicacion, style: ESTILO_UBICACION })
    ],
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
    this.direccionElegida = true;
    this.marcador.clear();
    this.marcador.addFeature(new Feature(new Point(punto)));
    const sinMovimiento = globalThis.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
    this.mapa.getView().animate({ center: punto, zoom: ZOOM_DIRECCION, duration: sinMovimiento ? 0 : 600 });
  }

  quitarMarcador(): void {
    this.marcador.clear();
  }

  /** Como SIATE al abrir el mapa: lo centra en la posición del usuario y la marca. Pide la posición una sola vez. */
  ubicarUsuario(): void {
    const geolocalizacion = globalThis.navigator?.geolocation;
    if (this.ubicacionPedida || !geolocalizacion) {
      return;
    }
    this.ubicacionPedida = true;
    geolocalizacion.getCurrentPosition(
      ({ coords }) => this.mostrarUbicacion(coords.longitude, coords.latitude),
      // Si el usuario negó el permiso no se le avisa: ya sabe por qué no aparece.
      (error) => this.sinUbicacion.set(error.code !== PERMISO_DENEGADO),
      OPCIONES_UBICACION
    );
  }

  private mostrarUbicacion(longitud: number, latitud: number): void {
    const punto = fromLonLat([longitud, latitud]);
    this.ubicacion.clear();
    this.ubicacion.addFeature(new Feature(new Point(punto)));
    // Si mientras tanto buscó una dirección, no se le mueve el mapa.
    if (!this.direccionElegida) {
      this.mapa.getView().setCenter(punto);
      this.mapa.getView().setZoom(esPantallaChica() ? ZOOM_UBICACION_PANTALLA_CHICA : ZOOM_UBICACION);
    }
  }

  montar(elemento: HTMLElement): void {
    this.mapa.setTarget(elemento);
  }

  desmontar(): void {
    this.mapa.setTarget(undefined);
  }
}
