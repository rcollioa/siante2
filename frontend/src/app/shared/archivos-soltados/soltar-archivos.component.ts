import { ChangeDetectionStrategy, Component, DOCUMENT, inject, signal } from '@angular/core';
import { ArchivosSoltados } from './archivos-soltados.service';

/** Permite soltar un archivo en cualquier parte de la ventana, sobre el mapa o el menú. El diálogo que acepta recibirlo
 * se marca con `data-recibe-archivos`; con otro diálogo abierto no pasa nada, para no interrumpir lo que está haciendo. */
@Component({
  selector: 'app-soltar-archivos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:dragstart)': 'arrastreInterno = true',
    '(document:dragend)': 'arrastreInterno = false',
    '(document:dragenter)': 'alArrastrar($event)',
    '(document:dragover)': 'alArrastrar($event)',
    '(document:dragleave)': 'alSalir($event)',
    '(document:drop)': 'alSoltar($event)'
  },
  template: `
    @if (cubrir()) {
      <div class="aviso"><p>Suelte el archivo para crear una capa</p></div>
    }
  `,
  styles: `
    .aviso {
      position: fixed;
      inset: 0;
      z-index: 20;
      display: flex;
      align-items: center;
      justify-content: center;
      outline: 3px dashed var(--color-primary);
      outline-offset: -0.75rem;
      background: rgb(76 111 237 / 0.18);
      /* Si recibiera el arrastre, el navegador avisaría que se salió del elemento de abajo. */
      pointer-events: none;
    }

    p {
      margin: 0;
      padding: 0.75rem 1.25rem;
      border-radius: var(--radius-md);
      background: var(--color-surface);
      color: var(--color-ink);
      font-weight: 600;
      box-shadow: var(--shadow-md);
    }
  `
})
export class SoltarArchivosComponent {
  private readonly documento = inject(DOCUMENT);
  private readonly soltados = inject(ArchivosSoltados);
  protected readonly cubrir = signal(false);
  /** Chrome presenta una imagen de la propia página como si fuera un archivo. */
  protected arrastreInterno = false;

  protected alArrastrar(evento: DragEvent): void {
    if (!this.traeArchivos(evento)) {
      return;
    }
    // Sin esto el navegador reemplaza la página por el archivo, y se perderían los cambios sin guardar.
    evento.preventDefault();
    const dialogos = Array.from(this.documento.querySelectorAll('dialog[open]'));
    const recibe = dialogos.every((dialogo) => dialogo.hasAttribute('data-recibe-archivos'));
    if (evento.dataTransfer) {
      evento.dataTransfer.dropEffect = recibe ? 'copy' : 'none';
    }
    this.soltados.arrastrando.set(recibe);
    this.cubrir.set(dialogos.length === 0);
  }

  /** Solo al salir de la ventana: entre un elemento y otro siempre hay un elemento de destino. */
  protected alSalir(evento: DragEvent): void {
    if (!evento.relatedTarget) {
      this.terminar();
    }
  }

  protected alSoltar(evento: DragEvent): void {
    if (!this.traeArchivos(evento)) {
      return;
    }
    evento.preventDefault();
    const recibe = this.soltados.arrastrando();
    this.terminar();
    const archivos = Array.from(evento.dataTransfer?.files ?? []);
    if (recibe && archivos.length > 0) {
      this.soltados.pendientes.set(archivos);
    }
  }

  private traeArchivos(evento: DragEvent): boolean {
    return !this.arrastreInterno && Array.from(evento.dataTransfer?.types ?? []).includes('Files');
  }

  private terminar(): void {
    this.soltados.arrastrando.set(false);
    this.cubrir.set(false);
  }
}
