import { Type } from '@angular/core';

/** Secciones del menú de SIATE. Sin `ruta` ni `panel` todavía no están migradas y se muestran como "Próximamente". */
export interface Seccion {
  nombre: string;
  ruta?: string;
  /** Lista que se despliega bajo la sección, como en el menú de SIATE; se descarga al abrirla por primera vez. */
  panel?: () => Promise<Type<unknown>>;
}

export const SECCIONES: Seccion[] = [
  { nombre: 'Inicio', ruta: '/mapa' },
  {
    nombre: 'Mis capas',
    panel: () => import('../../features/capas/mis-capas.component').then((m) => m.MisCapasComponent)
  },
  { nombre: 'Favoritos' },
  { nombre: 'Capas formales' },
  { nombre: 'Grupos de capas' },
  { nombre: 'Mis reportes compartidos' },
  { nombre: 'Mis reportes compartidos (grupos de capas)' },
  { nombre: 'Ayuda', ruta: '/ayuda' }
];
