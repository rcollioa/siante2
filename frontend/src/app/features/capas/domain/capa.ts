import { ElementoMapa } from '../../mapa/domain/mapa';

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

export interface Capa extends ResumenCapa {
  propia: boolean;
  tipo: TipoCapa;
  elementos: ElementoCapa[];
}

/** La capa no existe o el usuario no puede verla: la API no distingue los casos, para no revelar que existe. */
export class CapaNoEncontrada extends Error {
  constructor(readonly id: number) {
    super(`Capa ${id} no encontrada`);
    this.name = 'CapaNoEncontrada';
  }
}
