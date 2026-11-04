import { FormaElemento } from '../../mapa/domain/mapa';
import { ElementoCapa, Metadato } from './capa';

/** Lo dibujado, cambiado y borrado desde la última vez que se guardó: en SIATE, nada se escribe hasta "Guardar". */
export interface CambiosElementos {
  nuevos: readonly ElementoCapa[];
  modificados: readonly ElementoCapa[];
  eliminados: readonly number[];
}

export const SIN_CAMBIOS: CambiosElementos = { nuevos: [], modificados: [], eliminados: [] };

/** Un elemento recién dibujado, con un ID provisional negativo hasta que se guarda. */
export function elementoNuevo(idProvisional: number, forma: FormaElemento): ElementoCapa {
  return { ...forma, id: idProvisional, nombre: '', descripcion: '', estilo: { color: '', grosor: 0, relleno: '', icono: '' }, metadatos: [] };
}

export function conNuevo(cambios: CambiosElementos, elemento: ElementoCapa): CambiosElementos {
  return { ...cambios, nuevos: [...cambios.nuevos, elemento] };
}

/** Uno nuevo sigue siendo nuevo; uno ya guardado queda una sola vez entre los modificados. */
export function conCambio(cambios: CambiosElementos, elemento: ElementoCapa): CambiosElementos {
  if (elemento.id < 0) {
    return { ...cambios, nuevos: cambios.nuevos.map((nuevo) => (nuevo.id === elemento.id ? elemento : nuevo)) };
  }
  return { ...cambios, modificados: [...cambios.modificados.filter(({ id }) => id !== elemento.id), elemento] };
}

/** Borrar uno que nunca se guardó no deja rastro. */
export function sinElemento(cambios: CambiosElementos, elemento: ElementoCapa): CambiosElementos {
  if (elemento.id < 0) {
    return { ...cambios, nuevos: cambios.nuevos.filter(({ id }) => id !== elemento.id) };
  }
  return {
    ...cambios,
    modificados: cambios.modificados.filter(({ id }) => id !== elemento.id),
    eliminados: [...cambios.eliminados, elemento.id]
  };
}

export function cantidadDe({ nuevos, modificados, eliminados }: CambiosElementos): number {
  return nuevos.length + modificados.length + eliminados.length;
}

/** Sin los espacios a los lados ni los metadatos sin nombre, que no se guardan. */
export function limpios(cambios: CambiosElementos): CambiosElementos {
  return { ...cambios, nuevos: cambios.nuevos.map(limpio), modificados: cambios.modificados.map(limpio) };
}

function limpio(elemento: ElementoCapa): ElementoCapa {
  return {
    ...elemento,
    nombre: elemento.nombre.trim(),
    descripcion: elemento.descripcion.trim(),
    metadatos: elemento.metadatos.flatMap(limpiarMetadato)
  };
}

function limpiarMetadato({ nombre, valor }: Metadato): Metadato[] {
  return nombre.trim() ? [{ nombre: nombre.trim(), valor: valor.trim() }] : [];
}

/** Los elementos de la capa después de guardar: `idsNuevos` son los que asignó la base, en el orden de `nuevos`. */
export function aplicar(
  elementos: readonly ElementoCapa[],
  cambios: CambiosElementos,
  idsNuevos: readonly number[]
): ElementoCapa[] {
  const modificados = new Map(cambios.modificados.map((elemento) => [elemento.id, elemento]));
  return [
    ...elementos.filter(({ id }) => !cambios.eliminados.includes(id)).map((elemento) => modificados.get(elemento.id) ?? elemento),
    ...cambios.nuevos.map((elemento, indice) => ({ ...elemento, id: idsNuevos[indice] }))
  ];
}

/** Para `<input type="color">`, que solo entiende #rrggbb; vacío si el color no se puede convertir, como un nombre CSS. */
export function aHexadecimal(color: string): string {
  const corto = /^#([0-9a-f])([0-9a-f])([0-9a-f])$/i.exec(color);
  if (corto) {
    return `#${corto[1]}${corto[1]}${corto[2]}${corto[2]}${corto[3]}${corto[3]}`.toLowerCase();
  }
  if (/^#[0-9a-f]{6}$/i.test(color)) {
    return color.toLowerCase();
  }
  const rgb = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/i.exec(color);
  return rgb ? `#${[rgb[1], rgb[2], rgb[3]].map((parte) => Number(parte).toString(16).padStart(2, '0')).join('')}` : '';
}

/** Como SIATE ({@code hexToRgbA}): el color de fondo elegido queda semitransparente, para ver el mapa debajo. */
export function rellenoDe(hexadecimal: string): string {
  const [rojo, verde, azul] = [1, 3, 5].map((inicio) => parseInt(hexadecimal.slice(inicio, inicio + 2), 16));
  return `rgba(${rojo},${verde},${azul},0.5)`;
}
