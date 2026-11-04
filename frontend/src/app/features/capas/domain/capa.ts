import { ElementoMapa, Extension, GrillaWmts } from '../../mapa/domain/mapa';

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

/** Una capa del propio usuario en "Mis capas". Sin categoría, `categoriaId` es 0. */
export interface CapaPropia extends ResumenCapa {
  categoriaId: number;
}

/** Lo que el dueño puede elegir: SIATE solo ofrece público o privado. */
export type VisibilidadEditable = 'PRIVADA' | 'PUBLICA';

/** Los datos del formulario "Editar capa". */
export interface EdicionCapa {
  nombre: string;
  descripcion: string;
  categoriaId: number;
  visibilidad: VisibilidadEditable;
}

/** Los "Datos básicos" de una capa en blanco, como en SIATE: su categoría y su visibilidad se eligen al editarla. */
export interface NuevaCapa {
  nombre: string;
  descripcion: string;
}

/** Los límites de los formularios de SIATE; el backend exige los mismos. */
export const MINIMO_NOMBRE = 4;
export const MAXIMO_NOMBRE = 100;
export const MAXIMO_DESCRIPCION = 200;
export const MAXIMO_URL = 2000;

/** Los límites de un elemento; el backend exige los mismos. Holgados: la descripción de un KML puede ser una tabla en HTML. */
export const MAXIMO_NOMBRE_ELEMENTO = 500;
export const MAXIMO_TEXTO_ELEMENTO = 65536;
export const MAXIMO_METADATOS = 1000;

/** Qué falta corregir en cada campo; vacío si está bien. */
export interface ErroresEdicion {
  nombre: string;
  descripcion: string;
  categoria: string;
}

/** Solo la forma: qué tipo de servidor es y si se puede mostrar lo responde el backend. */
export function errorDeUrl(url: string): string {
  const limpia = url.trim();
  if (limpia.length === 0) {
    return 'URL requerida.';
  }
  if (limpia.length > MAXIMO_URL) {
    return `La URL no puede tener más de ${MAXIMO_URL} caracteres.`;
  }
  return /^https?:\/\/[^\s/?#]+\S*$/i.test(limpia) ? '' : 'Debe ser una URL válida.';
}

/** Sin contar los espacios a los lados, que no se guardan: un nombre de solo espacios no es un nombre. */
export function errorDeNombre(nombre: string): string {
  const largo = nombre.trim().length;
  if (largo === 0) {
    return 'Nombre requerido.';
  }
  if (largo < MINIMO_NOMBRE) {
    return `El nombre debe tener al menos ${MINIMO_NOMBRE} caracteres.`;
  }
  return largo > MAXIMO_NOMBRE ? `El nombre no puede tener más de ${MAXIMO_NOMBRE} caracteres.` : '';
}

export function errorDeDescripcion(descripcion: string): string {
  return descripcion.trim().length > MAXIMO_DESCRIPCION ? `La descripción no puede tener más de ${MAXIMO_DESCRIPCION} caracteres.` : '';
}

export function erroresDeEdicion({ nombre, descripcion, categoriaId }: EdicionCapa): ErroresEdicion {
  return {
    nombre: errorDeNombre(nombre),
    descripcion: errorDeDescripcion(descripcion),
    categoria: categoriaId > 0 ? '' : 'Categoría requerida.'
  };
}

/** Su dueño edita, publica o elimina solo capas privadas o públicas: ni formales ni áreas de dibujo. */
export function sePuedeGestionar({ visibilidad }: ResumenCapa): boolean {
  return visibilidad === 'PRIVADA' || visibilidad === 'PUBLICA';
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
  /** Dónde se piden las imágenes o los mosaicos del mapa: el backend, nunca el servidor directo. */
  urlMapa: string;
  /** Solo en WMTS: la grilla de sus mosaicos. */
  grilla: GrillaWmts | null;
  /** false si el servidor no respondió al abrir la capa: como en SIATE, se abre igual, vacía, con un aviso. */
  disponible: boolean;
}

export interface Capa extends ResumenCapa {
  propia: boolean;
  /** Su dueño puede dibujar en ella: no es formal ni un área de dibujo, y sus elementos no vienen de una URL. */
  editable: boolean;
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

/** El backend rechazó los cambios de una capa, con un mensaje para el usuario. */
export class CambiosRechazados extends Error {
  constructor(mensaje: string) {
    super(mensaje);
    this.name = 'CambiosRechazados';
  }
}

/** La capa no existe o el usuario no puede verla: la API no distingue los casos, para no revelar que existe. */
export class CapaNoEncontrada extends Error {
  constructor(readonly id: number) {
    super(`Capa ${id} no encontrada`);
    this.name = 'CapaNoEncontrada';
  }
}
