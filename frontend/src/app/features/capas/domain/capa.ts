import { ElementoMapa, Extension } from '../../mapa/domain/mapa';

/** Quién ve una capa ({@code VISIBILITY} de SIATE). Las áreas de dibujo son capas internas de los grupos. */
export type Visibilidad = 'PRIVADA' | 'PUBLICA' | 'FORMAL' | 'AREA_DE_DIBUJO';

/** VECTORIAL: elementos guardados en la base de SIATE. URL: un servicio WMS, WFS o WMTS externo. */
export type TipoCapa = 'VECTORIAL' | 'URL';

export const VISIBILIDADES: Record<Visibilidad, string> = {
  PRIVADA: 'Privada',
  PUBLICA: 'Pública',
  FORMAL: 'Formal',
  AREA_DE_DIBUJO: 'Área de dibujo'
};

export interface ResumenCapa {
  id: number;
  nombre: string;
  descripcion: string;
  visibilidad: Visibilidad;
}

export interface Metadato {
  nombre: string;
  valor: string;
}

/** Un elemento de la capa: lo que el mapa dibuja, más su información. */
export interface ElementoCapa extends ElementoMapa {
  id: number;
  nombre: string;
  descripcion: string;
  metadatos: Metadato[];
}

/** Pestañas del catálogo de SIATE. */
export type SeccionCatalogo = 'PUBLICAS' | 'MIAS' | 'FORMALES';

export const SECCIONES_CATALOGO: Record<SeccionCatalogo, string> = {
  PUBLICAS: 'Todas',
  MIAS: 'Usuario',
  FORMALES: 'Capas formales'
};

/** Una capa en el catálogo: para buscarla y elegirla. */
export interface CapaCatalogo extends ResumenCapa {
  categoria: string;
  dueno: string;
  favorita: boolean;
}

/** Como el buscador del catálogo de SIATE, pero sin importar tildes ni mayúsculas: "rios" encuentra "Ríos". */
export function coincide(capa: CapaCatalogo, texto: string): boolean {
  const buscado = sinTildes(texto.trim());
  return buscado === '' || [capa.nombre, capa.descripcion, capa.categoria, capa.dueno].some((campo) => sinTildes(campo).includes(buscado));
}

function sinTildes(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

/** Tipo de servidor de mapas de una capa por URL. */
export type TipoServicio = 'WMS' | 'WMTS' | 'WFS';

/** Una capa que publica el servidor de mapas. */
export interface CapaDeServicio {
  nombre: string;
  titulo: string;
  extension: Extension | null;
}

/** El servidor de mapas de una capa por URL. */
export interface ServicioCapa {
  tipo: TipoServicio;
  capas: CapaDeServicio[];
  /** Dónde se piden las imágenes del mapa: el backend, nunca el servidor directo. */
  urlMapa: string;
  /** false si el servidor no respondió al abrir la capa: como en SIATE, se abre igual, vacía, con un aviso. */
  disponible: boolean;
}

export interface Capa extends ResumenCapa {
  propia: boolean;
  tipo: TipoCapa;
  elementos: ElementoCapa[];
  /** Solo en las capas por URL. */
  servicio: ServicioCapa | null;
}

/** Un elemento que el servidor de mapas encontró en el punto consultado: el título de la capa del servidor en que
 * está (vacío si no lo informa) y sus datos, en el orden en que los entregó. */
export interface ElementoDeServicio {
  capa: string;
  datos: Metadato[];
}

/** El rectángulo que cubre todas las capas del servidor; null si ninguna informa su extensión. */
export function extensionDe(servicio: ServicioCapa): Extension | null {
  const extensiones = servicio.capas.flatMap(({ extension }) => (extension ? [extension] : []));
  if (extensiones.length === 0) {
    return null;
  }
  return {
    oeste: Math.min(...extensiones.map((e) => e.oeste)),
    sur: Math.min(...extensiones.map((e) => e.sur)),
    este: Math.max(...extensiones.map((e) => e.este)),
    norte: Math.max(...extensiones.map((e) => e.norte))
  };
}

/** El servidor de mapas de la capa no respondió al abrirla. */
export class ServidorDeMapasNoDisponible extends Error {
  constructor(readonly id: number) {
    super(`El servidor de mapas de la capa ${id} no responde`);
    this.name = 'ServidorDeMapasNoDisponible';
  }
}

/** La capa no existe o el usuario no puede verla: la API no distingue los casos, para no revelar que existe. */
export class CapaNoEncontrada extends Error {
  constructor(readonly id: number) {
    super(`Capa ${id} no encontrada`);
    this.name = 'CapaNoEncontrada';
  }
}
