/** Secciones del menú de SIATE. Sin `ruta` todavía no están migradas y se muestran como "Próximamente". */
export interface Seccion {
  nombre: string;
  ruta?: string;
}

export const SECCIONES: Seccion[] = [
  { nombre: 'Inicio', ruta: '/mapa' },
  { nombre: 'Mis capas' },
  { nombre: 'Favoritos' },
  { nombre: 'Capas formales' },
  { nombre: 'Grupos de capas' },
  { nombre: 'Mis reportes compartidos' },
  { nombre: 'Mis reportes compartidos (grupos de capas)' },
  { nombre: 'Ayuda', ruta: '/ayuda' }
];
