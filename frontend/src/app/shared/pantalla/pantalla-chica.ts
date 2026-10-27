/** Igual que SIANTE (768 px): bajo este ancho el menú tapa el mapa y el mapa parte más alejado. */
export function esPantallaChica(): boolean {
  return globalThis.matchMedia?.('(max-width: 48rem)').matches ?? false;
}
