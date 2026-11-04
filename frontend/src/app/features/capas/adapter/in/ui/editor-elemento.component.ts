import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { COLOR_POR_DEFECTO, GROSOR_POR_DEFECTO, RELLENO_POR_DEFECTO } from '../../../../mapa/domain/mapa';
import { EdicionElementos } from '../../../application/edicion-elementos';
import { aHexadecimal, rellenoDe } from '../../../domain/cambios-elementos';
import { ElementoCapa, MAXIMO_NOMBRE_ELEMENTO, MAXIMO_TEXTO_ELEMENTO, Metadato } from '../../../domain/capa';

/** El panel de edición de SIATE para el elemento seleccionado: "General", "Estilos" y "Metadata". */
@Component({
  selector: 'app-editor-elemento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './editor-elemento.component.html',
  styleUrl: './editor-elemento.component.css'
})
export class EditorElementoComponent {
  private readonly edicion = inject(EdicionElementos);
  readonly elemento = input.required<ElementoCapa>();

  protected readonly maximoNombre = MAXIMO_NOMBRE_ELEMENTO;
  protected readonly maximoTexto = MAXIMO_TEXTO_ELEMENTO;
  protected readonly esPunto = computed(() => {
    const { geometria, radio } = this.elemento();
    return radio === 0 && (geometria.type === 'Point' || geometria.type === 'MultiPoint');
  });
  protected readonly tieneRelleno = computed(() => this.elemento().radio > 0 || this.elemento().geometria.type.includes('Polygon'));
  protected readonly color = computed(() => aHexadecimal(this.elemento().estilo.color || COLOR_POR_DEFECTO));
  protected readonly relleno = computed(() => aHexadecimal(this.elemento().estilo.relleno || RELLENO_POR_DEFECTO));
  protected readonly grosor = computed(() => this.elemento().estilo.grosor || GROSOR_POR_DEFECTO);

  protected texto(evento: Event): string {
    return (evento.target as HTMLInputElement | HTMLTextAreaElement).value;
  }

  protected cambiarNombre(evento: Event): void {
    this.edicion.cambiarDatos(this.elemento(), { nombre: this.texto(evento) });
  }

  protected cambiarDescripcion(evento: Event): void {
    this.edicion.cambiarDatos(this.elemento(), { descripcion: this.texto(evento) });
  }

  protected cambiarColor(evento: Event): void {
    this.cambiarEstilo({ color: this.texto(evento) });
  }

  protected cambiarRelleno(evento: Event): void {
    this.cambiarEstilo({ relleno: rellenoDe(this.texto(evento)) });
  }

  protected cambiarGrosor(evento: Event): void {
    const grosor = Number(this.texto(evento));
    if (grosor >= 1 && grosor <= 100) {
      this.cambiarEstilo({ grosor });
    }
  }

  protected cambiarMetadato(indice: number, cambio: Partial<Metadato>): void {
    this.cambiarMetadatos(this.elemento().metadatos.map((dato, i) => (i === indice ? { ...dato, ...cambio } : dato)));
  }

  protected agregarMetadato(): void {
    this.cambiarMetadatos([...this.elemento().metadatos, { nombre: '', valor: '' }]);
  }

  protected quitarMetadato(indice: number): void {
    this.cambiarMetadatos(this.elemento().metadatos.filter((_, i) => i !== indice));
  }

  private cambiarEstilo(cambio: Partial<ElementoCapa['estilo']>): void {
    this.edicion.cambiarDatos(this.elemento(), { estilo: { ...this.elemento().estilo, ...cambio } });
  }

  private cambiarMetadatos(metadatos: Metadato[]): void {
    this.edicion.cambiarDatos(this.elemento(), { metadatos });
  }
}
