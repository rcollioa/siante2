import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';
import XYZ from 'ol/source/XYZ';
import { CapaBase } from '../../../domain/mapa';

/** Capas base del mapa, las mismas de SIATE; solo una visible a la vez. */
export function crearCapasBase(): Record<CapaBase, TileLayer> {
  return {
    mapa: new TileLayer({ source: new OSM() }),
    satelital: new TileLayer({
      visible: false,
      source: new XYZ({
        url: 'https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
        attributions:
          'Powered by Esri. Source: Esri, DigitalGlobe, GeoEye, Earthstar Geographics, CNES/Airbus DS, ' +
          'USDA, USGS, AeroGRID, IGN, and the GIS User Community',
        maxZoom: 19
      })
    })
  };
}
