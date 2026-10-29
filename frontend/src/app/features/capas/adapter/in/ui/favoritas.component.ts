import { ChangeDetectionStrategy, Component, computed, inject, input, viewChild } from '@angular/core';
import { Favoritas } from '../../../application/favoritas';
import { SeccionCatalogo } from '../../../domain/capa';
import { CatalogoCapasComponent } from './catalogo-capas.component';
import { ListaCapasComponent } from './lista-capas.component';

/** Lo que distingue a "Favoritos" de "Capas formales" en el menú. */
interface Variante {
  seccion: SeccionCatalogo;
  boton: string;
  descripcion: string;
  vacia: string;
}

const VARIANTES: Record<'favoritas' | 'formales', Variante> = {
  favoritas: {
    seccion: 'PUBLICAS',
    boton: 'Buscar en el catálogo',
    descripcion: 'sus favoritos',
    vacia: 'Aún no tiene capas favoritas.'
  },
  formales: {
    seccion: 'FORMALES',
    boton: 'Ver el catálogo de capas formales',
    descripcion: 'sus capas formales',
    vacia: 'Aún no ha elegido capas formales.'
  }
};

/** "Favoritos" y "Capas formales" en el menú, como en SIATE: la lista elegida por el usuario y el catálogo para
 * encontrar más. */
@Component({
  selector: 'app-favoritas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ListaCapasComponent, CatalogoCapasComponent],
  template: `
    <button type="button" class="abrir-catalogo" (click)="catalogo().abrir(variante().seccion)">{{ variante().boton }}</button>
    <app-lista-capas
      [capas]="tipo() === 'formales' ? favoritas.formales() : favoritas.favoritas()"
      [estado]="favoritas.estado()"
      [descripcion]="variante().descripcion"
      [vacia]="variante().vacia"
      (reintentar)="favoritas.cargar()" />
    <app-catalogo-capas />
  `,
  styles: `
    .abrir-catalogo {
      margin: 0.25rem 0.75rem 0.35rem;
      padding: 0;
      border: 0;
      background: transparent;
      color: var(--color-primary-ink);
      font: inherit;
      font-size: 0.875rem;
      text-decoration: underline;
      cursor: pointer;
    }

    .abrir-catalogo:focus-visible {
      outline: 2px solid var(--color-primary);
      outline-offset: 2px;
    }
  `
})
export class FavoritasComponent {
  readonly tipo = input<'favoritas' | 'formales'>('favoritas');
  protected readonly favoritas = inject(Favoritas);
  protected readonly catalogo = viewChild.required(CatalogoCapasComponent);
  protected readonly variante = computed(() => VARIANTES[this.tipo()]);

  constructor() {
    this.favoritas.cargar();
  }
}

/** "Capas formales": la misma sección, con las formales. Sin entradas, para el panel del menú. */
@Component({
  selector: 'app-capas-formales',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FavoritasComponent],
  template: `<app-favoritas tipo="formales" />`
})
export class CapasFormalesComponent {}
