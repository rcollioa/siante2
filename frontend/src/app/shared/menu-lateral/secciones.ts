import { Type } from '@angular/core';

/** Secciones del menú de SIATE. Sin `ruta` ni `panel` todavía no están migradas y se muestran como "Próximamente". */
export interface Seccion {
  nombre: string;
  ruta?: string;
  /** Lista que se despliega bajo la sección, como en el menú de SIATE; se descarga al abrirla por primera vez. */
  panel?: () => Promise<Type<unknown>>;
  /** Un archivo soltado en la ventana se entrega a este panel, que se despliega para recibirlo. */
  recibeArchivos?: boolean;
}

export const SECCIONES: Seccion[] = [
  { nombre: 'Inicio', ruta: '/mapa' },
  {
    nombre: 'Mis capas',
    recibeArchivos: true,
    panel: () => import('../../features/capas/adapter/in/ui/mis-capas.component').then((m) => m.MisCapasComponent)
  },
  {
    nombre: 'Favoritos',
    panel: () => import('../../features/capas/adapter/in/ui/favoritas.component').then((m) => m.FavoritasComponent)
  },
  {
    nombre: 'Capas formales',
    panel: () => import('../../features/capas/adapter/in/ui/favoritas.component').then((m) => m.CapasFormalesComponent)
  },
  { nombre: 'Grupos de capas' },
  { nombre: 'Mis reportes compartidos' },
  { nombre: 'Mis reportes compartidos (grupos de capas)' },
  { nombre: 'Ayuda', ruta: '/ayuda' }
];
